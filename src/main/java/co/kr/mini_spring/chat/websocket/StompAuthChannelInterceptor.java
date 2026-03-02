package co.kr.mini_spring.chat.websocket;

import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.security.CustomUserDetailsService;
import co.kr.mini_spring.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";
    private static final String BLACKLIST_PREFIX = "bl:access:";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = resolveAccessToken(accessor);
        if (token == null || token.isBlank()) {
            throw new MessagingException(ResponseCode.UNAUTHENTICATED.getMessage());
        }

        JwtTokenProvider.JwtValidationResult validation = jwtTokenProvider.validateTokenWithResult(token);
        if (!validation.isValid()) {
            throw new MessagingException(validation.getErrorCode().getMessage());
        }

        if (isBlacklisted(token)) {
            throw new MessagingException(ResponseCode.INVALID_TOKEN.getMessage());
        }

        String email = jwtTokenProvider.getEmail(token);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        accessor.setUser(authentication);
        log.debug("[STOMP CONNECT 인증 성공] email={}", email);
        return message;
    }

    private String resolveAccessToken(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) {
            return null;
        }
        Object token = accessor.getSessionAttributes().get(ACCESS_TOKEN_ATTRIBUTE);
        return token instanceof String ? (String) token : null;
    }

    private boolean isBlacklisted(String token) {
        Boolean exists = stringRedisTemplate.hasKey(BLACKLIST_PREFIX + token);
        return Boolean.TRUE.equals(exists);
    }
}

