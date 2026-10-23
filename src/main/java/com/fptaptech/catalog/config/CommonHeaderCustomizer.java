package com.fptaptech.catalog.config;

import com.fptaptech.catalog.common.RequestIdFilter;
import com.fptaptech.catalog.order.OrderController;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
@Order(20)
public class CommonHeaderCustomizer implements GlobalOperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getResponses() == null) {
            operation.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
        }
        operation.getResponses().forEach((code, response) -> addRequestId(response));
        if (OrderController.class.isAssignableFrom(handlerMethod.getBeanType())
                && "create".equals(handlerMethod.getMethod().getName())) {
            boolean present = operation.getParameters() != null && operation.getParameters().stream()
                    .anyMatch(parameter -> "Idempotency-Key".equals(parameter.getName()));
            if (!present) {
                operation.addParametersItem(new Parameter()
                        .name("Idempotency-Key")
                        .in("header")
                        .required(true)
                        .description("Bắt buộc khi tạo đơn. Cùng key và cùng raw body trong 24 giờ trả lại response lần đầu. Cùng key khác body trả 409 IDEMPOTENCY_CONFLICT.")
                        .schema(new StringSchema().minLength(1).maxLength(200))
                        .example("order-2026-10-16-lan-001"));
            }
        }
        return operation;
    }

    private static void addRequestId(ApiResponse response) {
        if (response.getHeaders() != null && response.getHeaders().containsKey(RequestIdFilter.HEADER)) {
            return;
        }
        response.addHeaderObject(RequestIdFilter.HEADER, new Header()
                .description("Định danh tương quan. Server echo giá trị client gửi, hoặc sinh UUID nếu thiếu.")
                .schema(new StringSchema().format("uuid")));
    }
}
