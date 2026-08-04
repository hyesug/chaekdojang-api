package com.chaekdojang.api.global.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAlertServiceTest {

    @Mock
    ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    JavaMailSender mailSender;

    private AdminAlertService adminAlertService;

    @BeforeEach
    void setUp() {
        adminAlertService = new AdminAlertService(mailSenderProvider);
        ReflectionTestUtils.setField(adminAlertService, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(adminAlertService, "fromEmail", "sender@example.com");
        ReflectionTestUtils.setField(adminAlertService, "mailHost", "smtp.gmail.com");
        ReflectionTestUtils.setField(adminAlertService, "mailUsername", "sender@example.com");
    }

    @Test
    @DisplayName("책도장 신규 회원가입 시 관리자에게 가입자 정보 메일을 보낸다")
    void sendSignupAlert_sendsMailToAdmin() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        adminAlertService.sendSignupAlert("reader@example.com", "새독자");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("sender@example.com");
        assertThat(message.getTo()).containsExactly("admin@example.com");
        assertThat(message.getSubject()).isEqualTo("[책도장] 신규 가입");
        assertThat(message.getText()).contains("닉네임: 새독자", "이메일: reader@example.com");
    }
}
