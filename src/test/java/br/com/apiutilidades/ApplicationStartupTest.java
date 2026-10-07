package br.com.apiutilidades;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"openweather.api-key=test-key", "api.jwt-secret=test-api-secret"})
@AutoConfigureMockMvc
class ApplicationStartupTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsWithConfiguredWeatherApiKey() {
    }

    @Test
    void servesApiLandingPageAtRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("API Utilidades")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/enderecos/{cep}")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/enderecos/busca")));
    }
}