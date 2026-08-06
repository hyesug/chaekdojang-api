package com.chaekdojang.api.global.filter;

import com.chaekdojang.api.domain.accesslog.AccessLogService;
import com.chaekdojang.api.global.util.MonitoringRequestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RequestLoggingFilterTest {

    @Test
    void codexMonitorRequestIsNotSavedAsAccessLog() throws Exception {
        AccessLogService accessLogService = mock(AccessLogService.class);
        RequestLoggingFilter filter = new RequestLoggingFilter(accessLogService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books/public/319");
        request.addHeader(MonitoringRequestUtils.HEADER, MonitoringRequestUtils.CODEX_MONITOR);

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        verifyNoInteractions(accessLogService);
    }
}
