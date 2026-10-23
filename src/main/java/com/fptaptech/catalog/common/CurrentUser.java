package com.fptaptech.catalog.common;

import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUser(Long customerId, String email, String role) {

    public boolean admin() {
        return "ADMIN".equals(role);
    }

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(customerId(jwt), jwt.getSubject(), jwt.getClaimAsString("role"));
    }

    private static Long customerId(Jwt jwt) {
        Object value = jwt.getClaim("customerId");
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }
}
