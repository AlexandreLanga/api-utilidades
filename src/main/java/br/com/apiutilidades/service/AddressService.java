package br.com.apiutilidades.service;

import br.com.apiutilidades.client.ViaCepClient;
import br.com.apiutilidades.dto.AddressResponse;
import br.com.apiutilidades.exception.AddressNotFoundException;
import br.com.apiutilidades.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AddressService {

    private final ViaCepClient viaCepClient;

    public AddressService(ViaCepClient viaCepClient) {
        this.viaCepClient = viaCepClient;
    }

    @Cacheable(cacheNames = "addresses", key = "#cep")
    @CircuitBreaker(name = "viacep", fallbackMethod = "fallback")
    public AddressResponse findByCep(String cep) {
        ViaCepClient.ViaCepResponse address = viaCepClient.findByCep(cep);
        if (address.erro()) {
            throw new AddressNotFoundException("CEP não encontrado.");
        }
        return new AddressResponse(address.cep(), address.logradouro(), address.complemento(), address.bairro(),
                address.localidade(), address.uf(), address.ibge(), address.ddd());
    }

    public AddressResponse fallback(String cep, Throwable exception) {
        if (exception instanceof AddressNotFoundException notFound) {
            throw notFound;
        }
        if (exception instanceof CallNotPermittedException) {
            throw new ExternalServiceException("O serviço ViaCEP está temporariamente indisponível.", exception);
        }
        throw new ExternalServiceException("Não foi possível consultar o ViaCEP.", exception);
    }
}