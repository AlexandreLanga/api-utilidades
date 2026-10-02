package br.com.apiutilidades.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean(name="viaCepRestClient")
    RestClient viaCepRestClient(RestClient.Builder builder) {
        return createClient(builder, "https://viacep.com.br");
    }

    @Bean(name="openWeatherRestClient")
    RestClient openWeatherRestClient(RestClient.Builder builder) {
        return createClient(builder, "https://api.openweathermap.org");
    }

    private RestClient createClient(RestClient.Builder builder, String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return builder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}