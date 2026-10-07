package br.com.apiutilidades.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.apiutilidades.config.WeatherProperties;
import br.com.apiutilidades.exception.CityNotFoundException;
import br.com.apiutilidades.exception.ExternalServiceException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExternalClientsTest {

    @Test
    void viaCepMapsSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ViaCepClient client = new ViaCepClient(builder.build());
        server.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withSuccess("""
                        {"cep":"01001-000","logradouro":"Praça da Sé","complemento":"lado ímpar","bairro":"Sé","localidade":"São Paulo","uf":"SP","ibge":"3550308","ddd":"11"}
                        """, MediaType.APPLICATION_JSON));

        ViaCepClient.ViaCepResponse response = client.findByCep("01001000");

        assertEquals("01001-000", response.cep());
        assertEquals("São Paulo", response.localidade());
        assertEquals("SP", response.uf());
        server.verify();
    }

    @Test
    void viaCepSearchesByAddressAndEncodesPathParameters() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ViaCepClient client = new ViaCepClient(builder.build());
        server.expect(requestTo(endsWith("/ws/RS/Porto%20Alegre/Domingos%20Jos%C3%A9/json/")))
                .andRespond(withSuccess("""
                        [{"cep":"90010-150","logradouro":"Rua dos Andradas","complemento":"","bairro":"Centro Histórico","localidade":"Porto Alegre","uf":"RS","ibge":"4314902","ddd":"51"}]
                        """, MediaType.APPLICATION_JSON));

        List<ViaCepClient.ViaCepResponse> response =
                client.findByAddress("RS", "Porto Alegre", "Domingos José");

        assertEquals(1, response.size());
        assertEquals("90010-150", response.getFirst().cep());
        assertEquals("Porto Alegre", response.getFirst().localidade());
        server.verify();
    }

    @Test
    void viaCepRejectsEmptyResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ViaCepClient client = new ViaCepClient(builder.build());
        server.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceException.class, () -> client.findByCep("01001000"));
        server.verify();
    }

    @Test
    void viaCepWrapsProviderHttpErrors() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ViaCepClient client = new ViaCepClient(builder.build());
        server.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withServerError());

        assertThrows(ExternalServiceException.class, () -> client.findByCep("01001000"));
        server.verify();
    }

    @Test
    void openWeatherLooksUpCityAndMapsResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://weather.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenWeatherClient client = new OpenWeatherClient(builder.build(), new WeatherProperties("test-key"));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://weather.test/data/2.5/weather?")))
                .andRespond(withSuccess("""
                        {"name":"Chapecó","main":{"temp":18.2,"feels_like":17.4,"temp_min":16.0,"temp_max":20.0,"humidity":72},"wind":{"speed":3.1},"weather":[{"main":"Clouds","description":"nublado"}],"sys":{"country":"BR"}}
                        """, MediaType.APPLICATION_JSON));

        OpenWeatherClient.OpenWeatherResponse response = client.findByCity("Chapecó", "metric", "pt_br");

        assertEquals("Chapecó", response.name());
        assertEquals(72, response.main().humidity());
        server.verify();
    }

    @Test
    void openWeatherLooksUpCoordinates() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://weather.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenWeatherClient client = new OpenWeatherClient(builder.build(), new WeatherProperties("test-key"));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://weather.test/data/2.5/weather?")))
                .andRespond(withSuccess("""
                        {"name":"Chapecó","main":{"temp":18,"feels_like":17,"temp_min":16,"temp_max":20,"humidity":72},"wind":{"speed":3},"weather":[{"main":"Clouds","description":"nublado"}],"sys":{"country":"BR"}}
                        """, MediaType.APPLICATION_JSON));

        OpenWeatherClient.OpenWeatherResponse response = client.findByCoordinates(
                new BigDecimal("-27.1"), new BigDecimal("-52.6"), "metric", "pt_br");

        assertEquals("Chapecó", response.name());
        server.verify();
    }

    @Test
    void openWeatherMapsNotFoundToCityNotFound() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://weather.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenWeatherClient client = new OpenWeatherClient(builder.build(), new WeatherProperties("test-key"));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://weather.test/data/2.5/weather?")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(CityNotFoundException.class, () -> client.findByCity("Narnia", "metric", "pt_br"));
        server.verify();
    }

    @Test
    void openWeatherWrapsProviderErrorsAndEmptyResponses() {
        RestClient.Builder errorBuilder = RestClient.builder().baseUrl("https://weather.test");
        MockRestServiceServer errorServer = MockRestServiceServer.bindTo(errorBuilder).build();
        OpenWeatherClient errorClient = new OpenWeatherClient(errorBuilder.build(), new WeatherProperties("test-key"));
        errorServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://weather.test/data/2.5/weather?")))
                .andRespond(withServerError());
        assertThrows(ExternalServiceException.class,
                () -> errorClient.findByCity("Chapecó", "metric", "pt_br"));
        errorServer.verify();

        RestClient.Builder emptyBuilder = RestClient.builder().baseUrl("https://weather.test");
        MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(emptyBuilder).build();
        OpenWeatherClient emptyClient = new OpenWeatherClient(emptyBuilder.build(), new WeatherProperties("test-key"));
        emptyServer.expect(requestTo(org.hamcrest.Matchers.startsWith("https://weather.test/data/2.5/weather?")))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(ExternalServiceException.class,
                () -> emptyClient.findByCity("Chapecó", "metric", "pt_br"));
        emptyServer.verify();
    }

        @Test
        void openWeatherWrapsMalformedJsonAsProviderFailure() {
                RestClient.Builder builder = RestClient.builder().baseUrl("https://weather.test");
                MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
                OpenWeatherClient client = new OpenWeatherClient(builder.build(), new WeatherProperties("test-key"));
                server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://weather.test/data/2.5/weather?")))
                                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

                assertThrows(ExternalServiceException.class,
                                () -> client.findByCity("Chapecó", "metric", "pt_br"));
                server.verify();
        }
}