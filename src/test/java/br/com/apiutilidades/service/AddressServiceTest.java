package br.com.apiutilidades.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.apiutilidades.client.ViaCepClient;
import br.com.apiutilidades.dto.AddressResponse;
import br.com.apiutilidades.exception.AddressNotFoundException;
import br.com.apiutilidades.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    @Mock private ViaCepClient viaCepClient;
    @InjectMocks private AddressService addressService;

    @Test
    void mapsViaCepLocalidadeToCidade() {
        when(viaCepClient.findByCep("01001000")).thenReturn(new ViaCepClient.ViaCepResponse(
                "01001-000", "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP", "3550308", "11", false));
        AddressResponse response = addressService.findByCep("01001000");
        assertEquals("São Paulo", response.cidade());
        assertEquals("SP", response.uf());
        verify(viaCepClient).findByCep("01001000");
    }

    @Test
    void mapsViaCepAddressSearchResults() {
        when(viaCepClient.findByAddress("RS", "Porto Alegre", "Domingos"))
                .thenReturn(List.of(new ViaCepClient.ViaCepResponse(
                        "90010-150", "Rua dos Andradas", "", "Centro Histórico",
                        "Porto Alegre", "RS", "4314902", "51", false)));

        List<AddressResponse> response = addressService.findByAddress("rs", " Porto Alegre ", " Domingos ");

        assertEquals(1, response.size());
        assertEquals("90010-150", response.getFirst().cep());
        assertEquals("Porto Alegre", response.getFirst().cidade());
        verify(viaCepClient).findByAddress("RS", "Porto Alegre", "Domingos");
    }

    @Test
    void rejectsCepNotFoundByProvider() {
        when(viaCepClient.findByCep("99999999")).thenReturn(new ViaCepClient.ViaCepResponse(
                null, null, null, null, null, null, null, null, true));
        assertThrows(AddressNotFoundException.class, () -> addressService.findByCep("99999999"));
    }

    @Test
    void fallbackPreservesNotFoundAndWrapsUnexpectedErrors() {
        AddressNotFoundException notFound = new AddressNotFoundException("CEP não encontrado.");
        assertSame(notFound, assertThrows(AddressNotFoundException.class,
                () -> addressService.fallback("99999999", notFound)));

        assertThrows(ExternalServiceException.class,
                () -> addressService.fallback("01001000", new IllegalStateException("offline")));

        assertThrows(ExternalServiceException.class,
                () -> addressService.fallback("RS", "Porto Alegre", "Domingos", new IllegalStateException("offline")));
    }

    @Test
    void addressSearchFallbackPreservesNotFound() {
        AddressService service = new AddressService(viaCepClient);
        AddressNotFoundException notFound = new AddressNotFoundException("CEP não encontrado.");

        assertSame(notFound, assertThrows(AddressNotFoundException.class,
                () -> service.fallback("RS", "Porto Alegre", "Domingos", notFound)));
    }

    @Test
    void fallbackReportsOpenCircuitAsUnavailable() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("viacep-test");
        circuitBreaker.transitionToOpenState();
        Runnable guardedCall = CircuitBreaker.decorateRunnable(circuitBreaker, () -> { });
        CallNotPermittedException openCircuit = assertThrows(CallNotPermittedException.class, guardedCall::run);

        assertThrows(ExternalServiceException.class,
                () -> addressService.fallback("01001000", openCircuit));
    }

    @Test
    void addressSearchFallbackReportsOpenCircuitAsUnavailable() {
        AddressService service = new AddressService(viaCepClient);
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("viacep-address-test");
        circuitBreaker.transitionToOpenState();
        Runnable guardedCall = CircuitBreaker.decorateRunnable(circuitBreaker, () -> { });
        CallNotPermittedException openCircuit = assertThrows(CallNotPermittedException.class, guardedCall::run);

        assertThrows(ExternalServiceException.class,
                () -> service.fallback("RS", "Porto Alegre", "Domingos", openCircuit));
    }
}