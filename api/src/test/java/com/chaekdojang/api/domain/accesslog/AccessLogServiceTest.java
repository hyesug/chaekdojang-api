package com.chaekdojang.api.domain.accesslog;

import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessLogServiceTest {

    @Mock AccessLogRepository accessLogRepository;
    @Mock UserRepository userRepository;
    @InjectMocks AccessLogService accessLogService;

    @Test
    void adminAccessIsStoredForIndividualActivityInvestigation() {
        User admin = User.create("admin@example.com", "admin", null);
        admin.promoteToAdmin();
        when(userRepository.findById(8L)).thenReturn(Optional.of(admin));

        accessLogService.save("203.0.113.10", 8L, "GET", "/api/feed", 200, 12,
                "Mozilla/5.0", "device-1");

        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(accessLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(admin);
    }
}
