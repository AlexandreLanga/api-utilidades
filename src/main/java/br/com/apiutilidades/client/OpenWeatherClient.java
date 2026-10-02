package br.com.apiutilidades.client;

import br.com.apiutilidades.config.WeatherProperties;
import br.com.apiutilidades.exception.CityNotFoundException;
import br.com.apiutilidades.exception.ExternalServiceException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.UnaryOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

@Component
public class OpenWeatherClient {

    private final RestClient restClient;
    private final WeatherProperties properties;

    public OpenWeatherClient(
            @Qualifier("openWeatherRestClient") RestClient restClient,
            WeatherProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public OpenWeatherResponse findByCity(String city, String unit, String language) {
        return execute(uri -> uri.queryParam("q", city), unit, language);
    }

    public OpenWeatherResponse findByCoordinates(
            BigDecimal latitude, BigDecimal longitude, String unit, String language) {
        return execute(uri -> uri.queryParam("lat", latitude).queryParam("lon", longitude), unit, language);
    }

    private OpenWeatherResponse execute(UnaryOperator<UriBuilder> query, String unit, String language) {
        try {
            OpenWeatherResponse response = restClient.get()
                    .uri(uriBuilder -> query.apply(uriBuilder.path("/data/2.5/weather"))
                            .queryParam("units", unit)
                            .queryParam("lang", language)
                            .queryParam("appid", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(OpenWeatherResponse.class);
            if (response == null) {
                throw new ExternalServiceException("O serviço de clima retornou uma resposta vazia.");
            }
            return response;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new CityNotFoundException("Localidade não encontrada no serviço de clima.");
            }
            throw new ExternalServiceException("Falha ao consultar o serviço de clima.", exception);
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Falha ao consultar o serviço de clima.", exception);
        }
    }

    public record OpenWeatherResponse(String name, Main main, Wind wind, List<WeatherCondition> weather, SystemInfo sys) {
        public record Main(double temp, double feels_like, double temp_min, double temp_max, int humidity) {
        }
        public record Wind(double speed) {
        }
        public record WeatherCondition(String main, String description) {
        }
        public record SystemInfo(String country) {
        }
    }
}