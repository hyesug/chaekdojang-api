package com.chaekdojang.api.global.security;

import com.chaekdojang.api.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 로그인 성공 후 Spring Security가 내부에서 사용하는 principal 객체.
 * JWT 발급에 필요한 userId를 꺼낼 수 있도록 User를 감싸고 있다.
 */
public class OAuthUserPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;
    private final boolean isNew;
    private final boolean linked;

    public OAuthUserPrincipal(User user, Map<String, Object> attributes, boolean isNew, boolean linked) {
        this.user = user;
        this.attributes = attributes;
        this.isNew = isNew;
        this.linked = linked;
    }

    public Long getUserId() {
        return user.getId();
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isLinked() { return linked; }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }
}
