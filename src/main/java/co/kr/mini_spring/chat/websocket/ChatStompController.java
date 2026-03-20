package co.kr.mini_spring.chat.websocket;

import co.kr.mini_spring.chat.dto.request.ChatSendMessageRequest;
import co.kr.mini_spring.chat.dto.response.ChatMessageResponse;
import co.kr.mini_spring.chat.dto.response.ChatWsErrorResponse;
import co.kr.mini_spring.chat.service.ChatMessageService;
import co.kr.mini_spring.chat.websocket.ChatEventPublisher;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.security.MemberAdapter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final ChatEventPublisher chatEventPublisher;

    @MessageMapping("/chat/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Valid @Payload ChatSendMessageRequest request,
                            Principal principal) {
        Long senderId = extractUserId(principal);
        log.info("[STOMP SEND 요청] roomId={}, senderId={}, clientMessageId={}, type={}, contentLength={}",
                roomId, senderId, request.getClientMessageId(), request.getType(),
                request.getContent() == null ? 0 : request.getContent().length());
        ChatMessageResponse response = chatMessageService.sendMessage(roomId, senderId, request);
        chatEventPublisher.publishMessageEvent(roomId, response);
        log.info("[STOMP SEND 브로드캐스트 완료] roomId={}, messageId={}, senderId={}, eventType={}",
                roomId, response.getMessageId(), senderId, response.getEventType());
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ChatWsErrorResponse handleBusinessException(BusinessException e) {
        log.warn("[STOMP 비즈니스 예외] code={}, message={}", e.getResponseCode().getCode(), e.getMessage());
        return new ChatWsErrorResponse(e.getResponseCode(), e.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ChatWsErrorResponse handleException(Exception e) {
        log.warn("[STOMP 예외] {}", e.getMessage());
        return new ChatWsErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR);
    }

    private Long extractUserId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }
        Object principalObject = authentication.getPrincipal();
        if (!(principalObject instanceof MemberAdapter memberAdapter)) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }
        return memberAdapter.getMember().getId();
    }
}
