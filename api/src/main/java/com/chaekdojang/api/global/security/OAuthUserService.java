package com.chaekdojang.api.global.security;

import com.chaekdojang.api.domain.user.AuthProvider;
import com.chaekdojang.api.domain.user.OAuthRegistrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;

/**
 * Spring Security가 OAuth2 콜백을 받은 뒤 이 서비스를 호출해 사용자 정보를 가져온다.
 * DB에 계정이 없으면 자동으로 가입 처리한다.
 */
@Component
@RequiredArgsConstructor
public class OAuthUserService extends DefaultOAuth2UserService {

    public static final String OAUTH_LINK_USER_ID = "oauth_link_user_id";

    private static final Logger log = LoggerFactory.getLogger(OAuthUserService.class);

    private final OAuthRegistrationService registrationService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        String registrationId = request.getClientRegistration().getRegistrationId();
        try {
            OAuth2User oauthUser = super.loadUser(request);
            log.info("[OAuth2] provider={} attributes={}", registrationId, oauthUser.getAttributes().keySet());
            AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = requestAttributes.getRequest().getSession(false);
            Long linkUserId = session != null ? (Long) session.getAttribute(OAUTH_LINK_USER_ID) : null;
            if (session != null) session.removeAttribute(OAUTH_LINK_USER_ID);
            OAuthRegistrationService.RegistrationResult result = registrationService.getOrRegister(provider, oauthUser.getAttributes(), linkUserId);
            log.info("[OAuth2] 로그인 성공 provider={} userId={} isNew={}", registrationId, result.user().getId(), result.isNew());
            return new OAuthUserPrincipal(result.user(), oauthUser.getAttributes(), result.isNew(), result.linked());
        } catch (Exception e) {
            log.error("[OAuth2] loadUser 실패 provider={} error={}", registrationId, e.getMessage(), e);
            throw e;
        }
    }
}
