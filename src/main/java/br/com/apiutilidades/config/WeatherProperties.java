package br.com.apiutilidades.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "openweather")
public record WeatherProperties(
        @NotBlank(message = "Configure a variável de ambiente OPENWEATHER_API_KEY antes de iniciar a aplicação.")
        String apiKey) {
}