package br.com.apiutilidades.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.apiutilidades.config.ApiKeyConfig;
import br.com.apiutilidades.config.ApiKeyInterceptor;
import br.com.apiutilidades.config.CacheConfig;
import br.com.apiutilidades.dto.AddressResponse;
import br.com.apiutilidades.service.AddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AddressController.class, properties = {
        "openweather.api-key=test-key",
        "api.jwt-secret=test-api-secret",
        "api.rate-limit.requests=1",
        "api.rate-limit.window=1m"
})
@Import({ApiKeyConfig.class, CacheConfig.class})
class ApiRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddressService addressService;

    @Test
    void returnsTooManyRequestsAfterConfiguredLimit() throws Exception {
        when(addressService.findByCep("01001000"))
                .thenReturn(new AddressResponse("01001-000", "Praça da Sé", "", "Sé",
                        "São Paulo", "SP", "3550308", "11"));

        mockMvc.perform(get("/api/v1/enderecos/01001000")
                        .header(ApiKeyInterceptor.API_KEY_HEADER, "test-api-secret")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        }))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/enderecos/01001000")
                        .header(ApiKeyInterceptor.API_KEY_HEADER, "test-api-secret")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Limite de requisições excedido.")));
    }
}
