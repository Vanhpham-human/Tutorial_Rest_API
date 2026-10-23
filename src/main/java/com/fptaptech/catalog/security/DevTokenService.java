package com.fptaptech.catalog.security;

import com.fptaptech.catalog.customer.Customer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class DevTokenService {

    private final JwtEncoder encoder;
    private final Clock clock;

    public DevTokenService(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    public String issueCustomer(Customer customer) {
        return issue(customer.getEmail(), "CUSTOMER", customer.getId());
    }

    public String issueAdmin() {
        return issue("admin@example.com", "ADMIN", null);
    }

    private String issue(String subject, String role, Long customerId) {
        Instant now = Instant.now(clock);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("techshop")
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(12, ChronoUnit.HOURS))
                .claim("role", role);
        if (customerId != null) {
            claims.claim("customerId", customerId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
