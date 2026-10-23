package com.fptaptech.catalog.security;

import com.fptaptech.catalog.common.ProblemResponses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, PartnerKeyFilter partnerKeyFilter,
                                             ObjectMapper objectMapper, Clock clock) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/dev/**").permitAll()
                        .requestMatchers("/api/v1/products/**").permitAll()
                        .requestMatchers("/api/webhooks/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/invoice").hasRole("ADMIN")
                        .requestMatchers("/api/orders/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, ex) -> ProblemResponses.write(objectMapper, response,
                                ProblemResponses.create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized",
                                        "Thiếu hoặc sai token", URI.create(request.getRequestURI()), List.of(), Instant.now(clock)))))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, ex) -> ProblemResponses.write(objectMapper, response,
                                ProblemResponses.create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized",
                                        "Thiếu hoặc sai token", URI.create(request.getRequestURI()), List.of(), Instant.now(clock))))
                        .accessDeniedHandler((request, response, ex) -> ProblemResponses.write(objectMapper, response,
                                ProblemResponses.create(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden",
                                        "Không có quyền thực hiện thao tác này", URI.create(request.getRequestURI()), List.of(), Instant.now(clock)))))
                .addFilterAfter(partnerKeyFilter, SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}
