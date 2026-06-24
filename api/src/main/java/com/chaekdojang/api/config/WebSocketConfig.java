package com.chaekdojang.api.config;

import com.chaekdojang.api.global.security.JwtProvider;
import com.chaekdojang.api.global.security.AuthCookieService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtProvider jwtProvider;
    private final AuthCookieService authCookieService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독하는 prefix: /topic/chat/{bookId}
        registry.enableSimpleBroker("/topic");
        // 클라이언트가 서버로 보내는 prefix: /app/chat/{bookId}
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(frontendUrl, "http://localhost:3000")
                .setHandshakeHandler(cookieHandshakeHandler())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        String jwt = token.substring(7);
                        if (jwtProvider.validate(jwt)) {
                            Long userId = jwtProvider.extractUserId(jwt);
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            userId.toString(),
                                            null,
                                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                    );
                            accessor.setUser(auth);
                        }
                    }
                }
                return message;
            }
        });
    }

    private DefaultHandshakeHandler cookieHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(
                    ServerHttpRequest request,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes
            ) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    return authCookieService.readAccessToken(servletRequest.getServletRequest())
                            .filter(jwtProvider::validate)
                            .map(jwtProvider::extractUserId)
                            .map(userId -> new UsernamePasswordAuthenticationToken(
                                    userId.toString(),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            ))
                            .orElse(null);
                }
                return null;
            }
        };
    }
}
