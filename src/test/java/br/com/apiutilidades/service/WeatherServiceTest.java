package br.com.apiutilidades.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import br.com.apiutilidades.client.OpenWeatherClient;
import br.com.apiutilidades.client.OpenWeatherClient.OpenWeatherResponse;
import br.com.apiutilidades.dto.WeatherResponse;
import br.com.apiutilidades.exception.CityNotFoundException;
import br.com.apiutilidades.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.math.BigDecimal;
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

        @Test
        void mapsWeatherByCoordinates() {
        OpenWeatherResponse providerResponse = new OpenWeatherResponse(
            "Chapecó", new OpenWeatherResponse.Main(18.2, 17.4, 16.0, 20.0, 72),
            new OpenWeatherResponse.Wind(3.1),
            List.of(new OpenWeatherResponse.WeatherCondition("Clouds", "nublado")),
            new OpenWeatherResponse.SystemInfo("BR"));
        BigDecimal latitude = new BigDecimal("-27.1");
        BigDecimal longitude = new BigDecimal("-52.6");
        when(openWeatherClient.findByCoordinates(latitude, longitude, "metric", "pt_br"))
            .thenReturn(providerResponse);

        WeatherResponse response = weatherService.findByCoordinates(latitude, longitude, "metric", "pt_br");

        assertEquals("Chapecó", response.cidade());
        assertEquals("nublado", response.descricao());
        }

        @Test
        void rejectsIncompleteProviderPayload() {
        OpenWeatherResponse incomplete = new OpenWeatherResponse("Chapecó", null, null, List.of(), null);
        when(openWeatherClient.findByCity("Chapecó", "metric", "pt_br")).thenReturn(incomplete);

        assertThrows(ExternalServiceException.class,
            () -> weatherService.findByCity("Chapecó", "metric", "pt_br"));
        }

        @Test
        void fallbacksPreserveNotFoundAndTranslateFailures() {
        assertThrows(CityNotFoundException.class,
            () -> weatherService.fallback("Narnia", "metric", "pt_br",
                new CityNotFoundException("Localidade não encontrada.")));
        assertThrows(ExternalServiceException.class,
            () -> weatherService.fallback("Chapecó", "metric", "pt_br",
                new ExternalServiceException("indisponível")));
        assertThrows(ExternalServiceException.class,
            () -> weatherService.fallback("Chapecó", "metric", "pt_br",
                new IllegalStateException("falha")));
        }

        @Test
        void coordinateFallbackReportsOpenCircuitAsUnavailable() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("openweather-test");
        circuitBreaker.transitionToOpenState();
        Runnable guardedCall = CircuitBreaker.decorateRunnable(circuitBreaker, () -> { });
        CallNotPermittedException openCircuit = assertThrows(CallNotPermittedException.class, guardedCall::run);

        assertThrows(ExternalServiceException.class,
            () -> weatherService.fallback(new BigDecimal("-27.1"), new BigDecimal("-52.6"),
                "metric", "pt_br", openCircuit));
        }
}