package br.com.apiutilidades.client;

import br.com.apiutilidades.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ViaCepClient {

    private final RestClient restClient;

    public ViaCepClient(@Qualifier("viaCepRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ViaCepResponse findByCep(String cep) {
        try {
            ViaCepResponse response = restClient.get()
                    .uri("/ws/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResponse.class);
            if (response == null) {
                throw new ExternalServiceException("O ViaCEP retornou uma resposta vazia.");
            }
            return response;
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Falha ao consultar o serviço ViaCEP.", exception);
        }
    }

    public record ViaCepResponse(
            String cep, String logradouro, String complemento, String bairro,
            String localidade, String uf, String ibge, String ddd, boolean erro) {
    }
}