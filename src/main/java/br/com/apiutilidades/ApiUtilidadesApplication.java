package br.com.apiutilidades;

import br.com.apiutilidades.config.WeatherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(WeatherProperties.class)
public class ApiUtilidadesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiUtilidadesApplication.class, args);
    }
}