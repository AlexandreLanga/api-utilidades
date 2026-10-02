package br.com.apiutilidades.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import br.com.apiutilidades.client.OpenWeatherClient;
import br.com.apiutilidades.client.OpenWeatherClient.OpenWeatherResponse;
import br.com.apiutilidades.dto.WeatherResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {
    @Mock private OpenWeatherClient openWeatherClient;
    @InjectMocks private WeatherService weatherService;

    @Test
    void mapsWeatherProviderFieldsToPublicResponse() {
        OpenWeatherResponse providerResponse = new OpenWeatherResponse(
                "Chapecó", new OpenWeatherResponse.Main(18.2, 17.4, 16.0, 20.0, 72),
                new OpenWeatherResponse.Wind(3.1),
                List.of(new OpenWeatherResponse.WeatherCondition("Clouds", "nublado")),
                new OpenWeatherResponse.SystemInfo("BR"));
        when(openWeatherClient.findByCity("Chapecó", "metric", "pt_br")).thenReturn(providerResponse);
        WeatherResponse response = weatherService.findByCity("Chapecó", "metric", "pt_br");
        assertEquals("Chapecó", response.cidade());
        assertEquals("BR", response.pais());
        assertEquals(18.2, response.temperaturaAtual());
        assertEquals("nublado", response.descricao());
    }
}