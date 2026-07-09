package com.chaekdojang.api.domain.feedback;

import com.chaekdojang.api.domain.feedback.dto.FeedbackCommentRequest;
import com.chaekdojang.api.domain.feedback.dto.FeedbackCommentResponse;
import com.chaekdojang.api.domain.feedback.dto.FeedbackConfigResponse;
import com.chaekdojang.api.global.response.ApiResponse;
import com.chaekdojang.api.global.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;

    @GetMapping("/config")
    public ApiResponse<FeedbackConfigResponse> getConfig() {
        return ApiResponse.ok(feedbackService.getConfig());
    }

    @PostMapping("/review-comment")
    public ApiResponse<FeedbackCommentResponse> createReviewComment(
            @RequestBody @Valid FeedbackCommentRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(feedbackService.createReviewComment(
                request,
                ClientIpUtils.getClientIp(servletRequest)
        ));
    }
}
