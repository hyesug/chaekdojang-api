package com.chaekdojang.api.global.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAlertService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.notification.admin-email:}")
    private String adminEmail;

    public void sendSignupAlert(String userEmail, String nickname) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("가입 알림 이메일 발송 건너뜀: 메일 발송 설정이 없습니다.");
            return;
        }
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("가입 알림 이메일 발송 건너뜀: NOTIFICATION_ADMIN_EMAIL이 설정되지 않았습니다.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("[책도장] 신규 가입");
            message.setText("닉네임: " + nickname + "\n이메일: " + (userEmail != null ? userEmail : "없음"));
            mailSender.send(message);
            log.info("가입 알림 이메일 발송 완료: nickname={}", nickname);
        } catch (Exception e) {
            log.warn("가입 알림 이메일 발송 실패: {}", e.getMessage());
        }
    }
}
