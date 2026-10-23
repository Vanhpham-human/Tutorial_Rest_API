package com.fptaptech.catalog.common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public final class ProblemResponses {

    public static final String ERROR_BASE = "https://api.example.com/errors/";

    private ProblemResponses() {
    }

    public static ProblemDetail create(HttpStatus status, String code, String title, String detail,
                                        URI instance, List<ApiFieldError> errors, Instant timestamp) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERROR_BASE + slug(code)));
        problem.setTitle(title);
        problem.setInstance(instance);
        problem.setProperty("code", code);
        problem.setProperty("timestamp", timestamp);
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }

    public static void write(ObjectMapper mapper, HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), problem);
    }

    public static String slug(String code) {
        return code.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
