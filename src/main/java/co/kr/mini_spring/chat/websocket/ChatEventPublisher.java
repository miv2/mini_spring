package co.kr.mini_spring.chat.websocket;

import co.kr.mini_spring.chat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * STOMP 채팅 이벤트 브로드캐스트 전담 컴포넌트입니다.
 * HTTP 컨트롤러와 서비스 레이어가 STOMP 구현 세부사항에 의존하지 않도록 단일 창구를 제공합니다.
 */
@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private static final String ROOM_TOPIC_PREFIX = "/topic/chat/rooms/";

    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 해당 채팅방을 구독 중인 모든 클라이언트에게 메시지 이벤트를 브로드캐스트합니다.
     *
     * @param roomId   브로드캐스트 대상 채팅방 ID
     * @param response 전송할 메시지 응답 (MESSAGE 또는 MESSAGE_DELETED 이벤트)
     */
    public void publishMessageEvent(Long roomId, ChatMessageResponse response) {
        simpMessagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + roomId, response);
    }
}
