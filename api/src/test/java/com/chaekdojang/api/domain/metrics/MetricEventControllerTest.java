package com.chaekdojang.api.domain.metrics;

import com.chaekdojang.api.domain.metrics.dto.MetricEventRequest;
import com.chaekdojang.api.global.util.MonitoringRequestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MetricEventControllerTest {

    @Test
    void codexMonitorPageViewIsNotStored() {
        MetricEventService service = mock(MetricEventService.class);
        MetricEventController controller = new MetricEventController(service);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(MonitoringRequestUtils.HEADER, MonitoringRequestUtils.CODEX_MONITOR);
        MetricEventRequest event = new MetricEventRequest(
                "page_view", "monitor-session", "/books/319", null, 0,
                "desktop", "monitor-device", Map.of());

        controller.record(event, servletRequest);

        verifyNoInteractions(service);
    }
}
