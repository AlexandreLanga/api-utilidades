package br.com.apiutilidades.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.apiutilidades.dto.AddressResponse;
import br.com.apiutilidades.dto.WeatherResponse;
import br.com.apiutilidades.config.CacheConfig;
import br.com.apiutilidades.exception.GlobalExceptionHandler;
import br.com.apiutilidades.service.AddressService;
import br.com.apiutilidades.service.WeatherService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = {AddressController.class, WeatherController.class}, properties = "openweather.api-key=test-key")
@Import({GlobalExceptionHandler.class, CacheConfig.class})
class ApiValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    void acceptsMaskedCepAndPassesNormalizedValueToService() throws Exception {
        when(addressService.findByCep("01001000"))
                .thenReturn(new AddressResponse("01001-000", "Praça da Sé", "", "Sé", "São Paulo", "SP", "3550308", "11"));

        mockMvc.perform(get("/api/v1/enderecos/01001-000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidade").value("São Paulo"))
                .andExpect(jsonPath("$.uf").value("SP"));

        verify(addressService).findByCep("01001000");
    }

    @Test
    void rejectsMalformedCepWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/enderecos/123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchesAddressesByUfCityAndStreet() throws Exception {
        when(addressService.findByAddress("RS", "Porto Alegre", "Domingos José"))
                .thenReturn(List.of(new AddressResponse(
                        "90010-150", "Rua dos Andradas", "", "Centro Histórico",
                        "Porto Alegre", "RS", "4314902", "51")));

        mockMvc.perform(get("/api/v1/enderecos/busca")
                        .param("uf", "RS")
                        .param("cidade", "Porto Alegre")
                        .param("logradouro", "Domingos José"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cep").value("90010-150"))
                .andExpect(jsonPath("$[0].cidade").value("Porto Alegre"));

        verify(addressService).findByAddress("RS", "Porto Alegre", "Domingos José");
    }

    @Test
    void rejectsAddressSearchWithInvalidUfOrShortCityAndStreet() throws Exception {
        mockMvc.perform(get("/api/v1/enderecos/busca")
                        .param("uf", "Rio")
                        .param("cidade", "PO")
                        .param("logradouro", "Rua"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsOutOfRangeCoordinatesWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/clima/coordenadas").param("lat", "91").param("lon", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void appliesWeatherDefaultsAndReturnsNormalizedFields() throws Exception {
        when(weatherService.findByCity("Chapecó", "metric", "pt_br"))
                .thenReturn(new WeatherResponse("Chapecó", "BR", 18.2, 17.4, 16.0, 20.0, 72, 3.1, "Clouds", "nublado"));

        mockMvc.perform(get("/api/v1/clima/cidade").param("cidade", "Chapecó"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperaturaAtual").value(18.2))
                .andExpect(jsonPath("$.umidadePercentual").value(72))
                .andExpect(jsonPath("$.descricao").value("nublado"));

        verify(weatherService).findByCity("Chapecó", "metric", "pt_br");
    }

        @Test
        void acceptsValidCoordinatesAndReturnsWeather() throws Exception {
                when(weatherService.findByCoordinates(new BigDecimal("-27.1"), new BigDecimal("-52.6"), "metric", "pt_br"))
                                .thenReturn(new WeatherResponse("Chapecó", "BR", 18.2, 17.4, 16.0, 20.0, 72, 3.1, "Clouds", "nublado"));

                mockMvc.perform(get("/api/v1/clima/coordenadas")
                                                .param("lat", "-27.1")
                                                .param("lon", "-52.6"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.cidade").value("Chapecó"));

                verify(weatherService).findByCoordinates(
                                new BigDecimal("-27.1"), new BigDecimal("-52.6"), "metric", "pt_br");
        }
}