package br.com.apiutilidades;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "openweather.api-key=test-key")
class ApplicationStartupTest {

    @Test
    void startsWithConfiguredWeatherApiKey() {
    }
}