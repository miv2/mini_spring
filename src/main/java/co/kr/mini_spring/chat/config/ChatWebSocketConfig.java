package co.kr.mini_spring.chat.config;

import co.kr.mini_spring.chat.websocket.ChatHandshakeInterceptor;
import co.kr.mini_spring.chat.websocket.ChatRoomAuthorizationChannelInterceptor;
import co.kr.mini_spring.chat.websocket.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatHandshakeInterceptor chatHandshakeInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final ChatRoomAuthorizationChannelInterceptor chatRoomAuthorizationChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOrigins(
                        "http://1.250.3.17:5173",
                        "https://minireact.duckdns.org"
                );
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                stompAuthChannelInterceptor,
                chatRoomAuthorizationChannelInterceptor
        );
    }
}

