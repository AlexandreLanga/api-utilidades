package br.com.apiutilidades.service;

import br.com.apiutilidades.client.ViaCepClient;
import br.com.apiutilidades.dto.AddressResponse;
import br.com.apiutilidades.exception.AddressNotFoundException;
import br.com.apiutilidades.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Locale;
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
        if (Boolean.TRUE.equals(address.erro())) {
            throw new AddressNotFoundException("CEP não encontrado.");
        }
        return toAddressResponse(address);
    }

    @CircuitBreaker(name = "viacep", fallbackMethod = "fallback")
    public List<AddressResponse> findByAddress(String uf, String cidade, String logradouro) {
        return viaCepClient.findByAddress(uf.trim().toUpperCase(Locale.ROOT), cidade.trim(), logradouro.trim())
                .stream()
                .map(AddressService::toAddressResponse)
                .toList();
    }

    public AddressResponse fallback(String cep, Throwable exception) {
        return handleFallback(exception);
    }

    public List<AddressResponse> fallback(String uf, String cidade, String logradouro, Throwable exception) {
        return handleFallback(exception);
    }

    private static AddressResponse toAddressResponse(ViaCepClient.ViaCepResponse address) {
        return new AddressResponse(address.cep(), address.logradouro(), address.complemento(), address.bairro(),
                address.localidade(), address.uf(), address.ibge(), address.ddd());
    }

    private static <T> T handleFallback(Throwable exception) {
        if (exception instanceof AddressNotFoundException notFound) {
            throw notFound;
        }
        if (exception instanceof CallNotPermittedException) {
            throw new ExternalServiceException("O serviço ViaCEP está temporariamente indisponível.", exception);
        }
        throw new ExternalServiceException("Não foi possível consultar o ViaCEP.", exception);
    }
}