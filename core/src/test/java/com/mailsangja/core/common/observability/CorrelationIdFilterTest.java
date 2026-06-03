package com.mailsangja.core.common.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter(new ObservabilitySupport());

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilter_요청헤더가있으면WorkId로사용하고응답헤더에반환한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ObservabilitySupport.CORRELATION_ID_HEADER, " request-work ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain filterChain = new CapturingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals("request-work", filterChain.workId);
        assertEquals("request-work", response.getHeader(ObservabilitySupport.CORRELATION_ID_HEADER));
        assertNull(MDC.get(ObservabilitySupport.WORK_ID));
    }

    @Test
    void doFilter_요청헤더가없으면WorkId를생성한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain filterChain = new CapturingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(filterChain.workId);
        assertEquals(filterChain.workId, response.getHeader(ObservabilitySupport.CORRELATION_ID_HEADER));
        assertNull(MDC.get(ObservabilitySupport.WORK_ID));
    }

    private static final class CapturingFilterChain extends MockFilterChain {

        private String workId;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            this.workId = MDC.get(ObservabilitySupport.WORK_ID);
        }
    }
}
