package br.com.apiutilidades.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRateLimitInterceptorTest {

    @Test
    void limitsRequestsPerClientAndReturnsRetryInformation() throws Exception {
        ApiRateLimitInterceptor interceptor = new ApiRateLimitInterceptor(2, Duration.ofMinutes(1));
        MockHttpServletRequest request = requestFrom("192.0.2.1");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, limitedResponse, new Object()));

        assertEquals(429, limitedResponse.getStatus());
        assertTrue(limitedResponse.getContentType().startsWith("application/problem+json"));
        assertEquals("2", limitedResponse.getHeader("RateLimit-Limit"));
        assertEquals("0", limitedResponse.getHeader("RateLimit-Remaining"));
        assertEquals("30", limitedResponse.getHeader("Retry-After"));
        assertTrue(limitedResponse.getContentAsString().contains("Limite de requisições excedido."));
    }

    @Test
    void tracksDifferentClientsIndependently() throws Exception {
        ApiRateLimitInterceptor interceptor = new ApiRateLimitInterceptor(1, Duration.ofMinutes(1));

        assertTrue(interceptor.preHandle(requestFrom("192.0.2.1"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(requestFrom("192.0.2.2"), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void rejectsInvalidRateLimitConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiRateLimitInterceptor(0, Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new ApiRateLimitInterceptor(10, Duration.ZERO));
    }

    private MockHttpServletRequest requestFrom(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }
}
