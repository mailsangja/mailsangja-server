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
    private static final String BACKEND_CORRELATION_ID = "019aa591-13e4-4ff4-a3b0-04c5d34fb085";

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilter_요청헤더가BackendGeneratedUuidV4이면WorkId로사용하고응답헤더에반환한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ObservabilitySupport.CORRELATION_ID_HEADER, " " + BACKEND_CORRELATION_ID + " ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain filterChain = new CapturingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(BACKEND_CORRELATION_ID, filterChain.workId);
        assertEquals(BACKEND_CORRELATION_ID, response.getHeader(ObservabilitySupport.CORRELATION_ID_HEADER));
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

    @Test
    void doFilter_요청헤더가UuidV4가아니면새WorkId를생성한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ObservabilitySupport.CORRELATION_ID_HEADER, "request-work");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain filterChain = new CapturingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(filterChain.workId);
        assertEquals(filterChain.workId, response.getHeader(ObservabilitySupport.CORRELATION_ID_HEADER));
        assertEquals(4, java.util.UUID.fromString(filterChain.workId).version());
        assertNull(MDC.get(ObservabilitySupport.WORK_ID));
    }

    @Test
    void doFilter_요청헤더가CanonicalUuid가아니면새WorkId를생성한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ObservabilitySupport.CORRELATION_ID_HEADER, BACKEND_CORRELATION_ID.toUpperCase());
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain filterChain = new CapturingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(filterChain.workId);
        assertEquals(filterChain.workId, response.getHeader(ObservabilitySupport.CORRELATION_ID_HEADER));
        assertEquals(4, java.util.UUID.fromString(filterChain.workId).version());
        assertNull(MDC.get(ObservabilitySupport.WORK_ID));
    }

    @Test
    void doFilter_요청헤더가없고MdcWorkId가BackendGeneratedUuidV4이면Fallback으로사용한다() throws Exception {
        MDC.put(ObservabilitySupport.WORK_ID, BACKEND_CORRELATION_ID);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain filterChain = new CapturingFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(BACKEND_CORRELATION_ID, filterChain.workId);
        assertEquals(BACKEND_CORRELATION_ID, response.getHeader(ObservabilitySupport.CORRELATION_ID_HEADER));
        assertEquals(BACKEND_CORRELATION_ID, MDC.get(ObservabilitySupport.WORK_ID));
    }

    private static final class CapturingFilterChain extends MockFilterChain {

        private String workId;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            this.workId = MDC.get(ObservabilitySupport.WORK_ID);
        }
    }
}
