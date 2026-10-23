# ADR 0001: Version chi tiết đơn bằng header `X-API-Version`

## Bối cảnh

`GET /api/orders/{code}` đổi biểu diễn tổng tiền: phiên bản 1 trả `totalAmount` là số, phiên bản 2 trả `total: {value, currency}`. Các thao tác còn lại (tạo đơn, danh sách, chuyển trạng thái, hóa đơn, webhook) giữ nguyên đường dẫn. Ba nhóm client — app khách, trang quản trị, đối tác vận chuyển — đã và sẽ sinh code từ spec.

Catalog sản phẩm dùng path `/api/v1/products` vì cả tập tài nguyên được version cùng lúc và không có hai biểu diễn song song.

## Quyết định

Đơn hàng version bằng header `X-API-Version`, không nhân đôi path.

- Thiếu header hoặc giá trị `1`: biểu diễn cũ, operation đánh `deprecated`, ngừng hỗ trợ `2027-04-01`.
- Giá trị `2`: biểu diễn `Money`.
- Giá trị khác: `400 UNSUPPORTED_API_VERSION`.
- Bốn nhóm spec (`public-v1`, `public-v2`, `admin`, `partner`) mô tả cùng các path, khác schema và tập action.

## Lý do

URL là định danh của đơn (`/api/orders/ORD-2026-000001`), còn version là lựa chọn biểu diễn. Header giữ một resource cho idempotency key, `ETag` / `If-Match`, hóa đơn và webhook. Đối tác không phải đổi URL khi khách chuyển từ v1 sang v2.

Path version (`/api/v1/orders` và `/api/v2/orders`) sẽ nhân bản mọi route chỉ để đổi một field, và làm ETag cùng mã đơn tồn tại ở hai chỗ. Cache và bookmark cũng bị tách.

Hai nhóm Swagger `public-v1` và `public-v2` khiến hợp đồng vẫn nhìn thấy trên UI dù header ít lộ hơn path. Mặc định vẫn là v1 đến ngày ngừng hỗ trợ, nên client cũ không gãy trong đêm cắt.

## Hệ quả

Client phải gửi header, không nhìn version trên URL. Spec ghi default, ngày sunset và example của cả hai schema. Bộ test hợp đồng khóa `deprecated`, schema `OrderDetailV1` / `OrderDetailV2`, và cấm hai schema lẫn vào nhau.
