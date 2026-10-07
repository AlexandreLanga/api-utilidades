package br.com.apiutilidades.client;

import br.com.apiutilidades.exception.ExternalServiceException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ViaCepClient {

    private static final ParameterizedTypeReference<List<ViaCepResponse>> ADDRESS_LIST_TYPE =
            new ParameterizedTypeReference<>() { };

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

    public List<ViaCepResponse> findByAddress(String uf, String cidade, String logradouro) {
        try {
            List<ViaCepResponse> response = restClient.get()
                    .uri("/ws/{uf}/{cidade}/{logradouro}/json/", uf, cidade, logradouro)
                    .retrieve()
                    .body(ADDRESS_LIST_TYPE);
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
            String localidade, String uf, String ibge, String ddd, Boolean erro) {
    }
}