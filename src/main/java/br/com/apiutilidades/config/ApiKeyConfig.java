package br.com.apiutilidades.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiKeyConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;

    public ApiKeyConfig(@Value("${api.jwt-secret}") String jwtSecret) {
        if (jwtSecret.isBlank()) {
            throw new IllegalStateException("A variável JWT_SECRET deve estar configurada.");
        }
        this.apiKeyInterceptor = new ApiKeyInterceptor(jwtSecret);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/api/**");
    }
}
