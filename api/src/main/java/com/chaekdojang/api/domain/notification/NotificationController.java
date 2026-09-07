package com.chaekdojang.api.domain.notification;

import com.chaekdojang.api.domain.notification.dto.NotificationResponse;
import com.chaekdojang.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "알림", description = "알림 조회·읽음·삭제")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @DeleteMapping("/{id}/campaign-subscription")
    public ApiResponse<Void> unsubscribeCampaignInvitation(@PathVariable Long id) {
        notificationService.unsubscribeCampaignInvitation(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "내 알림 목록", description = "최신순으로 반환합니다. JWT 필요.")
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        return ApiResponse.ok(notificationService.getMyNotifications());
    }

    @Operation(summary = "읽지 않은 알림 수", description = "JWT 필요.")
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.ok(notificationService.getUnreadCount());
    }

    @Operation(summary = "알림 읽음 처리", description = "JWT 필요.")
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "전체 읽음 처리", description = "JWT 필요.")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.ok();
    }

    @Operation(summary = "전체 삭제", description = "JWT 필요.")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() {
        notificationService.deleteAll();
    }

    @Operation(summary = "알림 삭제", description = "JWT 필요.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        notificationService.delete(id);
    }
}
