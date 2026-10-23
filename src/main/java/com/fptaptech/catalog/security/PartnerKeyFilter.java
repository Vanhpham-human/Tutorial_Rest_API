package com.fptaptech.catalog.security;

import com.fptaptech.catalog.common.ProblemResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class PartnerKeyFilter extends OncePerRequestFilter {

    private final String partnerKey;
    private final boolean protectPartnerDocs;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PartnerKeyFilter(@Value("${app.security.partner-key}") String partnerKey,
                            @Value("${app.security.protect-partner-docs:false}") boolean protectPartnerDocs,
                            ObjectMapper objectMapper, Clock clock) {
        this.partnerKey = partnerKey;
        this.protectPartnerDocs = protectPartnerDocs;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean webhook = path.startsWith("/api/webhooks/");
        boolean docs = protectPartnerDocs && isPartnerDocs(path);
        if ((webhook || docs) && !matches(request.getHeader("X-Partner-Key"))) {
            ProblemResponses.write(objectMapper, response, ProblemResponses.create(
                    HttpStatus.UNAUTHORIZED, "PARTNER_KEY_INVALID", "Invalid partner key",
                    "Header X-Partner-Key không hợp lệ", URI.create(path), List.of(), Instant.now(clock)));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isPartnerDocs(String path) {
        return "/v3/api-docs/partner".equals(path)
                || "/v3/api-docs/partner.yaml".equals(path)
                || "/v3/api-docs.yaml/partner".equals(path);
    }

    private boolean matches(String supplied) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8), partnerKey.getBytes(StandardCharsets.UTF_8));
    }
}
