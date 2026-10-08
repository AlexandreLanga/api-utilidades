package br.com.apiutilidades.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.servlet.HandlerInterceptor;

public class ApiKeyInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-API-KEY";

    private final byte[] expectedKey;

    ApiKeyInterceptor(String expectedKey) {
        this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {
        if (org.springframework.web.cors.CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String providedKey = request.getHeader(API_KEY_HEADER);
        if (providedKey != null
                && MessageDigest.isEqual(expectedKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
                        + "\"detail\":\"Chave de API ausente ou inválida.\"}");
        return false;
    }
}
