package co.kr.mini_spring.chat.domain.repository;

import co.kr.mini_spring.chat.domain.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    Optional<ConversationParticipant> findByConversationIdAndUserIdAndDeletedAtIsNull(Long conversationId, Long userId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    long countByConversationIdAndDeletedAtIsNull(Long conversationId);

    List<ConversationParticipant> findByConversationIdAndDeletedAtIsNull(Long conversationId);

    List<ConversationParticipant> findByConversationIdInAndDeletedAtIsNull(List<Long> conversationIds);
}
