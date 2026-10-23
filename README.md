# TechShop Catalog và Order API

REST API Spring Boot 4.1 (OpenAPI 3.1) gồm catalog sản phẩm và Order Service. Hợp đồng được tách thành bốn nhóm spec: `public-v1`, `public-v2`, `admin`, `partner`.

## Chạy

Cần JDK 25. MySQL cho môi trường dev do Docker Compose kéo lên (`mysql:8.4`). Test dùng H2, không cần Docker.

```bash
./mvnw spring-boot:run
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). Dropdown chỉ có bốn nhóm trên. Spec JSON/YAML:

- `/v3/api-docs/{group}` và `/v3/api-docs.yaml/{group}`
- Nhóm `partner` ở profile `prod` đòi header `X-Partner-Key`

```bash
./mvnw test
./mvnw test -Dupdate.openapi.specs=true
./mvnw verify
```

`test` kiểm tra nghiệp vụ, máy trạng thái và hợp đồng. Cờ `-Dupdate.openapi.specs=true` ghi lại `docs/api/openapi-*.yaml` cho khớp spec đang chạy. `verify` chạy Redocly (0 error) rồi sinh client TypeScript từ `public-v2` và `tsc --noEmit`.

## Dữ liệu và token dev

Flyway seed hai khách và hai sản phẩm:

| Email | Vai trò |
| --- | --- |
| lan@example.com | CUSTOMER |
| minh@example.com | CUSTOMER |

Sản phẩm: Bàn phím cơ K8 Pro (1.590.000), Chuột không dây M650 (790.000). Đơn 2× bàn phím + 1× chuột = 3.970.000 VND. Giá do server chụp, client không gửi giá.

Token chỉ có khi `app.security.dev-tokens-enabled=true` (mặc định dev, tắt ở `prod`):

```bash
curl -s -X POST http://localhost:8080/api/dev/tokens \
  -H 'Content-Type: application/json' \
  -d '{"email":"lan@example.com"}'

curl -s -X POST http://localhost:8080/api/dev/tokens \
  -H 'Content-Type: application/json' \
  -d '{"role":"ADMIN"}'
```

Gửi `Authorization: Bearer <token>`. Đối tác gửi `X-Partner-Key` (mặc định `dev-partner-key`).

## Biến môi trường

| Biến | Mặc định dev | Ý nghĩa |
| --- | --- | --- |
| `JWT_SECRET` | `techshop-dev-secret-key-32bytes!` | HMAC HS256, ít nhất 32 byte |
| `PARTNER_KEY` | `dev-partner-key` | Khóa webhook và spec partner khi bật bảo vệ |
| `SPRING_DATASOURCE_URL` | Docker Compose tự gán | Bắt buộc ở profile `prod` |
| `SPRING_DATASOURCE_USERNAME` | | |
| `SPRING_DATASOURCE_PASSWORD` | | |

Profile `prod` tắt Swagger UI, tắt token dev, và chặn `/v3/api-docs/partner` nếu thiếu `X-Partner-Key`. Các spec còn lại vẫn mở.

## Đơn hàng

`POST /api/orders` bắt buộc `Idempotency-Key`. Cùng key và cùng raw body trong 24 giờ trả lại đơn đã tạo. Cùng key, body khác: `409 IDEMPOTENCY_CONFLICT`. Khoảng trắng khác nhau là body khác.

`GET /api/orders/{code}` version bằng header `X-API-Version` (xem `docs/adr/0001-header-versioning.md`). Thiếu header hoặc `1` trả `totalAmount` số, deprecated, sunset `2027-04-01`. `2` trả `total: {value, currency}`. Catalog sản phẩm vẫn version bằng path `/api/v1/products`.

Mọi response có `X-Request-Id`. Chuyển trạng thái bắt buộc `If-Match` đúng ETag version (`"0"`, `0` hoặc `W/"0"`). Thiếu header: `428`. Lệch version: `412`.

Máy trạng thái:

- `PENDING` → `CONFIRM` (ADMIN) → `CONFIRMED` → `SHIP` (ADMIN, PARTNER) → `SHIPPING` → `DELIVER` (ADMIN, PARTNER) → `DELIVERED`
- `PENDING` → `CANCEL` (CUSTOMER đơn của mình, ADMIN)
- `CONFIRMED` → `CANCEL` (chỉ ADMIN, bắt buộc `reason`)
- `SHIPPING` → `FAIL_DELIVERY` (ADMIN, PARTNER) → `CONFIRMED`
- `DELIVERED` → `REQUEST_RETURN` (CUSTOMER đơn của mình hoặc ADMIN, trong 7 ngày) → `RETURN_REQUESTED` → `ACCEPT_RETURN` (ADMIN) → `RETURNED`

`CANCELLED` và `RETURNED` là trạng thái cuối. CUSTOMER gọi action khác: `403`. PARTNER gọi action ngoài `SHIP`, `DELIVER`, `FAIL_DELIVERY`: `409`. Đơn của khách khác: `404`.

Hóa đơn PDF tối đa 5MB, magic `%PDF`. Doanh thu ngày (`GET /api/admin/reports/daily-revenue`) cộng `DELIVERED` và `RETURN_REQUESTED` theo ngày `deliveredAt` UTC, điền 0 cho ngày trống, không tính `RETURNED`.

## Tiêu chí hợp đồng

| Mã | Nội dung |
| --- | --- |
| D1 | Bốn `GroupedOpenApi`, lọc method bằng `@ApiGroups` |
| D2 | `bearerAuth` (JWT) và `partnerKey` (`X-Partner-Key`), gắn từng operation |
| D3 | `Payment` là `oneOf` + discriminator `method` |
| D4 | `OrderSearchQuery`, sort chỉ `createdAt` hoặc `totalAmount` |
| D5 | `Idempotency-Key` và `X-Request-Id` do customizer, không lặp annotation |
| D6 | `ApiProblem`, mỗi operation chỉ liệt kê lỗi nó trả |
| D7 | Example tạo đơn: COD, CARD, BANK_TRANSFER |
| D8 | v1 deprecated + sunset `2027-04-01`; v2 dùng `Money` |
| D9 | `docs/api/*.yaml` khớp spec sống |
| D10 | Redocly 0 error ở `verify` |
| D11 | Client `typescript-fetch` từ public-v2, `tsc --noEmit` |
| D12 | Profile `prod` tắt Swagger UI, tắt token dev, khóa spec partner |
