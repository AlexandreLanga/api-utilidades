package br.com.apiutilidades.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiKeyConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final ApiRateLimitInterceptor apiRateLimitInterceptor;

    public ApiKeyConfig(
            @Value("${api.jwt-secret}") String jwtSecret,
            @Value("${api.rate-limit.requests:60}") int requests,
            @Value("${api.rate-limit.window:1m}") Duration window) {
        if (jwtSecret.isBlank()) {
            throw new IllegalStateException("A variável JWT_SECRET deve estar configurada.");
        }
        this.apiKeyInterceptor = new ApiKeyInterceptor(jwtSecret);
        this.apiRateLimitInterceptor = new ApiRateLimitInterceptor(requests, window);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://alexandrelanga.github.io")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(apiRateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
