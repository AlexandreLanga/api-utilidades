package br.com.apiutilidades.service;

import br.com.apiutilidades.client.OpenWeatherClient;
import br.com.apiutilidades.client.OpenWeatherClient.OpenWeatherResponse;
import br.com.apiutilidades.dto.WeatherResponse;
import br.com.apiutilidades.exception.CityNotFoundException;
import br.com.apiutilidades.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private final OpenWeatherClient openWeatherClient;

    public WeatherService(OpenWeatherClient openWeatherClient) {
        this.openWeatherClient = openWeatherClient;
    }

    @CircuitBreaker(name = "openweather", fallbackMethod = "fallback")
    public WeatherResponse findByCity(String city, String unit, String language) {
        return map(openWeatherClient.findByCity(city.trim(), unit, language));
    }

    @CircuitBreaker(name = "openweather", fallbackMethod = "fallback")
    public WeatherResponse findByCoordinates(
            BigDecimal latitude, BigDecimal longitude, String unit, String language) {
        return map(openWeatherClient.findByCoordinates(latitude, longitude, unit, language));
    }

    private WeatherResponse map(OpenWeatherResponse weather) {
        if (weather.main() == null || weather.wind() == null || weather.sys() == null
                || weather.weather() == null || weather.weather().isEmpty()) {
            throw new ExternalServiceException("O serviço de clima retornou dados incompletos.");
        }
        OpenWeatherResponse.WeatherCondition condition = weather.weather().get(0);
        return new WeatherResponse(weather.name(), weather.sys().country(), weather.main().temp(),
                weather.main().feels_like(), weather.main().temp_min(), weather.main().temp_max(),
                weather.main().humidity(), weather.wind().speed(), condition.main(), condition.description());
    }

    public WeatherResponse fallback(String city, String unit, String language, Throwable exception) {
        throw fallbackException(exception);
    }

    public WeatherResponse fallback(
            BigDecimal latitude, BigDecimal longitude, String unit, String language, Throwable exception) {
        throw fallbackException(exception);
    }

    private ExternalServiceException fallbackException(Throwable exception) {
        if (exception instanceof CityNotFoundException notFound) {
            throw notFound;
        }
        if (exception instanceof ExternalServiceException externalException) {
            return externalException;
        }
        if (exception instanceof CallNotPermittedException) {
            return new ExternalServiceException("O serviço de clima está temporariamente indisponível.", exception);
        }
        return new ExternalServiceException("Não foi possível consultar o serviço de clima.", exception);
    }
}