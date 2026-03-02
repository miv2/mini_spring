package co.kr.mini_spring.chat.domain.repository;

import co.kr.mini_spring.chat.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByConversationIdAndClientMessageId(Long conversationId, String clientMessageId);
}

