package br.com.apiutilidades.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsExternalAndUnexpectedErrorsToProblemDetails() {
        HttpServletRequest request = request("/api/v1/clima/cidade");

        ResponseEntity<ProblemDetail> external = handler.handleExternalService(
                new ExternalServiceException("Provedor indisponível."), request);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, external.getStatusCode());
        assertEquals("/api/v1/clima/cidade", external.getBody().getInstance().toString());

        ResponseEntity<ProblemDetail> unexpected = handler.handleUnexpected(new IllegalStateException(), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, unexpected.getStatusCode());
        assertEquals("Ocorreu um erro interno.", unexpected.getBody().getDetail());
    }

    @Test
    void mapsNotFoundAndInvalidParametersToProblemDetails() {
        HttpServletRequest request = request("/api/v1/enderecos/99999999");

        ResponseEntity<ProblemDetail> notFound = handler.handleNotFound(
                new AddressNotFoundException("CEP não encontrado."), request);
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());

        ResponseEntity<ProblemDetail> invalid = handler.handleConstraintViolation(
                new IllegalArgumentException("parâmetro inválido"), request);
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
    }

    @Test
    void includesFieldDetailsForInvalidRequestBody() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "cidade", "Cidade inválida."));
        MethodParameter parameter = new MethodParameter(
                getClass().getDeclaredMethod("invalidBody", String.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleInvalidBody(exception, request("/api/v1/clima"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("cidade: Cidade inválida.", response.getBody().getDetail());
    }

    private HttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private void invalidBody(String value) {
    }
}