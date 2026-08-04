package com.chaekdojang.api.domain.metrics;

import com.chaekdojang.api.domain.metrics.dto.MetricEventRequest;
import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.security.SecurityUtils;
import com.chaekdojang.api.global.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricEventController {

    private final MetricEventService metricEventService;

    @PostMapping("/events")
    public ApiResponse<Void> record(
            @RequestBody @Valid MetricEventRequest request,
            HttpServletRequest servletRequest) {
        metricEventService.record(request, ClientIpUtils.getClientIp(servletRequest),
                servletRequest.getHeader("User-Agent"), currentUserIdOrNull());
        return ApiResponse.ok(null);
    }

    private Long currentUserIdOrNull() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

}
