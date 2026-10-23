package com.fptaptech.catalog.security;

import com.fptaptech.catalog.common.CatalogException;
import com.fptaptech.catalog.customer.CustomerRepository;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
@RequestMapping("/api/dev/tokens")
@ConditionalOnProperty(name = "app.security.dev-tokens-enabled", havingValue = "true")
public class DevTokenController {

    private final DevTokenService tokens;
    private final CustomerRepository customers;

    public DevTokenController(DevTokenService tokens, CustomerRepository customers) {
        this.tokens = tokens;
        this.customers = customers;
    }

    @PostMapping
    public Map<String, String> issue(@RequestBody DevTokenRequest request) {
        if (request != null && "ADMIN".equalsIgnoreCase(request.role())) {
            return Map.of("token", tokens.issueAdmin(), "role", "ADMIN");
        }
        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed",
                    "Cần email của khách hoặc role ADMIN");
        }
        var customer = customers.findByEmail(request.email()).orElseThrow(() ->
                new CatalogException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found",
                        "Không tìm thấy khách " + request.email()));
        return Map.of("token", tokens.issueCustomer(customer), "role", "CUSTOMER");
    }

    public record DevTokenRequest(String email, String role) {
    }
}
