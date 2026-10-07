package br.com.apiutilidades.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class ApiRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final Duration CLIENT_EXPIRY = Duration.ofDays(1);

    private final int requestLimit;
    private final long windowNanos;
    private final Cache<String, TokenBucket> buckets;

    ApiRateLimitInterceptor(int requestLimit, Duration window) {
        if (requestLimit < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("O limite e a janela de rate limit devem ser positivos.");
        }
        this.requestLimit = requestLimit;
        this.windowNanos = window.toNanos();
        this.buckets = Caffeine.newBuilder()
                .maximumSize(MAX_TRACKED_CLIENTS)
                .expireAfterAccess(CLIENT_EXPIRY)
                .build();
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {
        TokenBucket bucket = buckets.get(request.getRemoteAddr(), ignored -> new TokenBucket(requestLimit));
        TokenBucket.Decision decision = bucket.tryAcquire(System.nanoTime(), requestLimit, windowNanos);

        response.setHeader("RateLimit-Limit", Integer.toString(requestLimit));
        response.setHeader("RateLimit-Remaining", Integer.toString(decision.remaining()));
        if (decision.allowed()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                        + "\"detail\":\"Limite de requisições excedido.\"}");
        return false;
    }

    private static final class TokenBucket {

        private double tokens;
        private long lastRefillNanos;

        private TokenBucket(int capacity) {
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        private synchronized Decision tryAcquire(long nowNanos, int capacity, long windowNanos) {
            long elapsedNanos = Math.max(0, nowNanos - lastRefillNanos);
            tokens = Math.min(capacity, tokens + (double) elapsedNanos * capacity / windowNanos);
            lastRefillNanos = nowNanos;

            if (tokens >= 1) {
                tokens -= 1;
                return new Decision(true, (int) Math.floor(tokens), 0);
            }

            long retryAfterSeconds = Math.max(1, (long) Math.ceil(
                    (1 - tokens) * windowNanos / capacity / TimeUnit.SECONDS.toNanos(1)));
            return new Decision(false, 0, retryAfterSeconds);
        }

        private record Decision(boolean allowed, int remaining, long retryAfterSeconds) {
        }
    }
}
