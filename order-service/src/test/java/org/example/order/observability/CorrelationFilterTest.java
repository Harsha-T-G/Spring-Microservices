package org.example.order.observability;

import java.util.UUID;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationFilterTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "bad value", "bad/value", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"})
    void givenMissingOrInvalidCorrelation_whenFiltering_thenGeneratedIdMatchesMdcAndHeader(String input)
            throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/example");
        var response = new MockHttpServletResponse();
        if (input != null) {
            request.addHeader("X-Correlation-Id", input);
        }
        new CorrelationFilter().doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            String id = response.getHeader("X-Correlation-Id");
            assertThat(UUID.fromString(id).toString()).isEqualTo(id);
            assertThat(MDC.get("correlationId")).isEqualTo(id);
        });
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void givenValidCorrelation_whenChainThrows_thenMdcIsClearedAndHeaderRetained() {
        var request = new MockHttpServletRequest("POST", "/api/v1/example");
        request.addHeader("X-Correlation-Id", "valid-correlation_1");
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> new CorrelationFilter().doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("valid-correlation_1");
            throw new ServletException("test-chain-failure");
        })).isInstanceOf(ServletException.class);
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("valid-correlation_1");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void givenDuplicateCorrelationHeaders_whenFiltering_thenNewIdIsGenerated() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/example");
        request.addHeader("X-Correlation-Id", "first");
        request.addHeader("X-Correlation-Id", "second");
        var response = new MockHttpServletResponse();
        new CorrelationFilter().doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get("correlationId")).isEqualTo(response.getHeader("X-Correlation-Id")));
        assertThat(UUID.fromString(response.getHeader("X-Correlation-Id"))).isNotNull();
        assertThat(MDC.get("correlationId")).isNull();
    }
}
