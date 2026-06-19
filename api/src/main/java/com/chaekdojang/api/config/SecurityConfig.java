package com.chaekdojang.api.config;

import com.chaekdojang.api.global.security.JwtAuthenticationFilter;
import com.chaekdojang.api.global.security.OAuthSuccessHandler;
import com.chaekdojang.api.global.security.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final OAuthUserService oAuthUserService;
    private final OAuthSuccessHandler oAuthSuccessHandler;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            // OAuth2 state 검증에 세션이 필요하므로 IF_REQUIRED 사용
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers(
                        "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs", "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/reviews/feed",
                        "/api/users/me",
                        "/api/users/*/follow/status"
                ).authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/api/metrics/events",
                        "/api/dev/login"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/reviews", "/api/reviews/**",
                        "/api/books/**",
                        "/api/users/*/followers", "/api/users/*/followings",
                        "/api/users/*/reviews",
                        "/api/users/nickname/*",
                        "/api/users/*"
                ).permitAll()
                // 문의: POST·GET 모두 비회원 허용 (권한 체크는 서비스 레이어에서)
                .requestMatchers("/api/inquiries/**").permitAll()
                // 관리자 API: 인증 필요 (권한 체크는 서비스 레이어에서)
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(ui -> ui.userService(oAuthUserService))
                .successHandler(oAuthSuccessHandler)
                .failureHandler((req, res, ex) -> {
                    log.error("[OAuth2 실패] provider={} error={} cause={}",
                            req.getServletPath(), ex.getMessage(),
                            ex.getCause() != null ? ex.getCause().getMessage() : "none");
                    res.sendRedirect(frontendUrl + "/auth/login?error=oauth_failed");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> res.sendError(401, "Unauthorized"))
            );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // @Component 필터가 서블릿 컨테이너에 이중 등록되는 것을 방지
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setEnabled(false);
        return bean;
    }
}
