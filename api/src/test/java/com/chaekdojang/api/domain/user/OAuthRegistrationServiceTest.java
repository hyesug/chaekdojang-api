package com.chaekdojang.api.domain.user;

import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.notification.AdminAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthRegistrationServiceTest {

    @Mock UserRepository users;
    @Mock UserAuthProviderRepository providers;
    @Mock AdminAlertService alerts;
    @InjectMocks OAuthRegistrationService service;

    @Test
    void emailWithoutProviderMatchDoesNotMergeDifferentSocialAccounts() {
        when(providers.findByProviderAndProviderUserId(any(), anyString())).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.getOrRegister(AuthProvider.GOOGLE, Map.of("sub", "google-1", "email", "same@example.com"));
        service.getOrRegister(AuthProvider.KAKAO, Map.of("id", 123L, "kakao_account", Map.of("email", "same@example.com")));

        verify(users, never()).findByEmail(anyString());
        ArgumentCaptor<UserAuthProvider> saved = ArgumentCaptor.forClass(UserAuthProvider.class);
        verify(providers, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(UserAuthProvider::getProvider)
                .containsExactly(AuthProvider.GOOGLE, AuthProvider.KAKAO);
    }

    @Test
    void providerIdentityReconnectsSameUserEvenWithoutEmail() {
        User user = User.create(null, "독자_1234", null);
        UserAuthProvider provider = UserAuthProvider.of(user, AuthProvider.KAKAO, "123", null, null);
        when(providers.findByProviderAndProviderUserId(AuthProvider.KAKAO, "123")).thenReturn(Optional.of(provider));

        OAuthRegistrationService.RegistrationResult result = service.getOrRegister(
                AuthProvider.KAKAO, Map.of("id", 123L, "kakao_account", Map.of()));

        assertThat(result.user()).isSameAs(user);
        assertThat(result.isNew()).isFalse();
        verify(users, never()).save(any());
    }

    @Test
    void cannotLinkProviderAlreadyOwnedByAnotherUser() {
        User owner = User.create(null, "독자_1234", null);
        ReflectionTestUtils.setField(owner, "id", 1L);
        UserAuthProvider provider = UserAuthProvider.of(owner, AuthProvider.GOOGLE, "google-1", null, null);
        when(providers.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-1")).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> service.getOrRegister(AuthProvider.GOOGLE,
                Map.of("sub", "google-1"), 99L))
                .isInstanceOf(CustomException.class);
    }
}
