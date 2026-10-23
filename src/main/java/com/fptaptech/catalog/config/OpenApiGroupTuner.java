package com.fptaptech.catalog.config;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class OpenApiGroupTuner {

    private static final String PUBLIC_ACTIONS = """
            CANCEL: CUSTOMER chỉ khi đơn của mình còn PENDING; không dùng cho CONFIRMED.
            REQUEST_RETURN: CUSTOMER trên đơn của mình, chỉ khi DELIVERED và trong 7 ngày kể từ lúc giao.
            """;
    private static final String ADMIN_ACTIONS = """
            CONFIRM: ADMIN, PENDING → CONFIRMED.
            CANCEL: ADMIN khi PENDING, hoặc CONFIRMED nếu có reason. CUSTOMER chỉ CANCEL khi PENDING.
            SHIP: ADMIN hoặc đối tác, CONFIRMED → SHIPPING.
            FAIL_DELIVERY: ADMIN hoặc đối tác, SHIPPING → CONFIRMED để giao lại.
            DELIVER: ADMIN hoặc đối tác, SHIPPING → DELIVERED.
            REQUEST_RETURN: CUSTOMER hoặc ADMIN, DELIVERED → RETURN_REQUESTED, trong 7 ngày.
            ACCEPT_RETURN: ADMIN, RETURN_REQUESTED → RETURNED.
            """;
    private static final String PARTNER_ACTIONS = """
            SHIP: đối tác, CONFIRMED → SHIPPING.
            DELIVER: đối tác, SHIPPING → DELIVERED.
            FAIL_DELIVERY: đối tác, SHIPPING → CONFIRMED (giao thất bại, giao lại).
            """;

    public void tune(OpenAPI openApi, String group, String serverUrl, String version, String sunset) {
        openApi.setServers(List.of(new Server().url(serverUrl).description("Môi trường hiện tại")));
        switch (group) {
            case "public-v1" -> {
                describe(openApi, "TechShop Public API v1", version,
                        "Hợp đồng public phiên bản 1. GET /api/orders/{code} trả totalAmount dạng số, deprecated, ngừng hỗ trợ vào "
                                + sunset + ". Dùng nhóm public-v2 với header X-API-Version: 2.");
                shapeOrderDetail(openApi, "OrderDetailV1", true,
                        "Deprecated. Ngừng hỗ trợ vào " + sunset + ". totalAmount là số. Chuyển sang X-API-Version: 2.");
                narrowActions(openApi, List.of("CANCEL", "REQUEST_RETURN"), PUBLIC_ACTIONS);
            }
            case "public-v2" -> {
                describe(openApi, "TechShop Public API v2", version,
                        "Hợp đồng public hiện hành. GET /api/orders/{code} với X-API-Version: 2 trả total là object {value, currency}.");
                shapeOrderDetail(openApi, "OrderDetailV2", false,
                        "total là object {value, currency}. Gửi header X-API-Version: 2.");
                narrowActions(openApi, List.of("CANCEL", "REQUEST_RETURN"), PUBLIC_ACTIONS);
            }
            case "admin" -> {
                describe(openApi, "TechShop Admin API", version,
                        "Hợp đồng quản trị. ADMIN thực hiện mọi action hợp lệ của máy trạng thái. Hủy đơn CONFIRMED bắt buộc có reason.");
                narrowActions(openApi, List.of(
                        "CONFIRM", "CANCEL", "SHIP", "FAIL_DELIVERY", "DELIVER", "REQUEST_RETURN", "ACCEPT_RETURN"),
                        ADMIN_ACTIONS);
            }
            case "partner" -> {
                describe(openApi, "TechShop Partner API", version,
                        "Hợp đồng đối tác vận chuyển. Xác thực bằng header X-Partner-Key.");
                narrowActions(openApi, List.of("SHIP", "DELIVER", "FAIL_DELIVERY"), PARTNER_ACTIONS);
            }
            default -> {
            }
        }
        retagInvoices(openApi);
        declareTags(openApi);
        prune(openApi);
    }

    private static void describe(OpenAPI openApi, String title, String version, String description) {
        openApi.getInfo().setTitle(title);
        openApi.getInfo().setVersion(version);
        openApi.getInfo().setDescription(description);
    }

    private static void shapeOrderDetail(OpenAPI openApi, String schemaName, boolean deprecated, String description) {
        PathItem item = openApi.getPaths() == null ? null : openApi.getPaths().get("/api/orders/{code}");
        if (item == null || item.getGet() == null) {
            return;
        }
        Operation operation = item.getGet();
        operation.setDeprecated(deprecated ? Boolean.TRUE : null);
        operation.setDescription(description);
        var ok = operation.getResponses().get("200");
        if (ok != null && ok.getContent() != null) {
            ok.getContent().forEach((type, media) -> media.setSchema(ref(schemaName)));
        }
    }

    private static void narrowActions(OpenAPI openApi, List<String> values, String description) {
        if (openApi.getComponents() == null) {
            return;
        }
        if (openApi.getComponents().getSchemas() == null) {
            openApi.getComponents().setSchemas(new java.util.LinkedHashMap<>());
        }
        StringSchema narrowed = new StringSchema();
        narrowed.setDescription(description.strip());
        narrowed.setEnum(values);
        openApi.getComponents().addSchemas("OrderAction", narrowed);
        for (Schema<?> schema : openApi.getComponents().getSchemas().values()) {
            pointActionAtOrderAction(schema);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void pointActionAtOrderAction(Schema<?> schema) {
        if (schema == null || schema.getProperties() == null) {
            return;
        }
        Object action = schema.getProperties().get("action");
        if (action instanceof Schema<?> actionSchema && isOrderAction(actionSchema)) {
            schema.getProperties().put("action", ref("OrderAction"));
        }
    }

    private static boolean isOrderAction(Schema<?> schema) {
        if (schema.get$ref() != null && schema.get$ref().endsWith("/OrderAction")) {
            return true;
        }
        List<?> values = schema.getEnum();
        return values != null && (values.contains("CANCEL") || values.contains("SHIP") || values.contains("CONFIRM"));
    }

    private static void retagInvoices(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        PathItem invoice = openApi.getPaths().get("/api/orders/{code}/invoice");
        if (invoice == null) {
            return;
        }
        if (invoice.getGet() != null) {
            invoice.getGet().setTags(List.of("Invoices"));
        }
        if (invoice.getPost() != null) {
            invoice.getPost().setTags(List.of("Invoices"));
        }
    }

    private static void declareTags(OpenAPI openApi) {
        Map<String, String> descriptions = Map.of(
                "Orders", "Đặt và theo dõi đơn hàng",
                "Invoices", "Hóa đơn PDF của đơn",
                "Shipping", "Webhook đối tác vận chuyển",
                "Reports", "Báo cáo nội bộ");
        Set<String> names = new LinkedHashSet<>();
        if (openApi.getPaths() != null) {
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                if (operation.getTags() != null) {
                    names.addAll(operation.getTags());
                }
            }));
        }
        openApi.setTags(names.stream()
                .map(name -> new Tag().name(name).description(descriptions.getOrDefault(name, name)))
                .toList());
    }

    private static void prune(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            return;
        }
        Set<String> used = new HashSet<>();
        collectRefs(Json.mapper().valueToTree(openApi.getPaths()), used);
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        if (schemas != null) {
            boolean grew = true;
            while (grew) {
                grew = false;
                for (String name : new ArrayList<>(used)) {
                    Schema<?> schema = schemas.get(name);
                    if (schema == null) {
                        continue;
                    }
                    int before = used.size();
                    collectRefs(Json.mapper().valueToTree(schema), used);
                    if (used.size() > before) {
                        grew = true;
                    }
                }
            }
            schemas.keySet().retainAll(used);
        }
        Set<String> schemes = new HashSet<>();
        if (openApi.getSecurity() != null) {
            openApi.getSecurity().forEach(requirement -> schemes.addAll(requirement.keySet()));
        }
        if (openApi.getPaths() != null) {
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                if (operation.getSecurity() != null) {
                    operation.getSecurity().forEach(requirement -> schemes.addAll(requirement.keySet()));
                }
            }));
        }
        if (openApi.getComponents().getSecuritySchemes() != null) {
            openApi.getComponents().getSecuritySchemes().keySet().retainAll(schemes);
        }
    }

    private static void collectRefs(JsonNode node, Set<String> used) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if ("$ref".equals(entry.getKey()) && entry.getValue().isTextual()) {
                    String ref = entry.getValue().asText();
                    String prefix = "#/components/schemas/";
                    if (ref.startsWith(prefix)) {
                        used.add(ref.substring(prefix.length()));
                    }
                } else {
                    collectRefs(entry.getValue(), used);
                }
            });
        } else if (node.isArray()) {
            node.forEach(child -> collectRefs(child, used));
        }
    }

    private static Schema<?> ref(String name) {
        Schema<?> schema = new Schema<>();
        schema.set$ref("#/components/schemas/" + name);
        return schema;
    }
}
