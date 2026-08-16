package com.example.anusha.job_trail.auth.security;

import com.example.anusha.job_trail.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Spring Security rejects unauthenticated requests before they ever reach
 * {@code GlobalExceptionHandler} (the security filter chain runs ahead of
 * the DispatcherServlet), so it needs its own path to the same
 * {@link ErrorResponse} shape — otherwise a missing/invalid token would 401
 * with Spring's default plain-text body instead of the app's JSON error
 * format.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Missing or invalid access token",
                request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
