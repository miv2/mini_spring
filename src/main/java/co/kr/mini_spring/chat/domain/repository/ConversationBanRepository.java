package co.kr.mini_spring.chat.domain.repository;

import co.kr.mini_spring.chat.domain.ConversationBan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationBanRepository extends JpaRepository<ConversationBan, Long> {

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);

    List<ConversationBan> findByConversationId(Long conversationId);
}

