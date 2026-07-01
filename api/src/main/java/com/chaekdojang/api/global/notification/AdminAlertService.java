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

    @Value("${app.notification.from-email:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendSignupAlert(String userEmail, String nickname) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("가입 알림 이메일 발송 건너뜀: JavaMailSender가 없습니다. spring.mail.host 설정 여부를 확인하세요. hostConfigured={}",
                    mailHost != null && !mailHost.isBlank());
            return;
        }
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("가입 알림 이메일 발송 건너뜀: NOTIFICATION_ADMIN_EMAIL이 설정되지 않았습니다.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }
            message.setTo(adminEmail);
            message.setSubject("[책도장] 신규 가입");
            message.setText("닉네임: " + nickname + "\n이메일: " + (userEmail != null ? userEmail : "없음"));
            mailSender.send(message);
            log.info("가입 알림 이메일 발송 완료: nickname={}", nickname);
        } catch (Exception e) {
            log.warn("가입 알림 이메일 발송 실패: hostConfigured={}, usernameConfigured={}, adminEmailConfigured={}, error={}",
                    mailHost != null && !mailHost.isBlank(),
                    mailUsername != null && !mailUsername.isBlank(),
                    adminEmail != null && !adminEmail.isBlank(),
                    e.getMessage());
        }
    }
}
