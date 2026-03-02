package co.kr.mini_spring.chat.domain.repository;

import co.kr.mini_spring.chat.domain.ConversationType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static co.kr.mini_spring.chat.domain.QConversation.conversation;
import static co.kr.mini_spring.chat.domain.QConversationBan.conversationBan;
import static co.kr.mini_spring.chat.domain.QConversationParticipant.conversationParticipant;

@Repository
@RequiredArgsConstructor
public class ChatPermissionQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<RoomAccess> getRoomAccess(Long conversationId, Long userId) {
        BooleanExpression ownerExpr = conversation.ownerId.eq(userId);
        BooleanExpression participantExpr = JPAExpressions.selectOne()
                .from(conversationParticipant)
                .where(
                        conversationParticipant.conversationId.eq(conversationId),
                        conversationParticipant.userId.eq(userId),
                        conversationParticipant.deletedAt.isNull()
                )
                .exists();
        BooleanExpression bannedExpr = JPAExpressions.selectOne()
                .from(conversationBan)
                .where(
                        conversationBan.conversationId.eq(conversationId),
                        conversationBan.userId.eq(userId)
                )
                .exists();

        Tuple tuple = queryFactory.select(ownerExpr, participantExpr, bannedExpr, conversation.type)
                .from(conversation)
                .where(
                        conversation.id.eq(conversationId),
                        conversation.deletedAt.isNull()
                )
                .fetchOne();

        if (tuple == null) {
            return Optional.empty();
        }

        return Optional.of(new RoomAccess(
                Boolean.TRUE.equals(tuple.get(ownerExpr)),
                Boolean.TRUE.equals(tuple.get(participantExpr)),
                Boolean.TRUE.equals(tuple.get(bannedExpr)),
                tuple.get(conversation.type)
        ));
    }

    public Optional<Long> findDirectPeerId(Long conversationId, Long currentUserId) {
        return Optional.ofNullable(
                queryFactory.select(conversationParticipant.userId)
                        .from(conversationParticipant)
                        .where(
                                conversationParticipant.conversationId.eq(conversationId),
                                conversationParticipant.userId.ne(currentUserId),
                                conversationParticipant.deletedAt.isNull()
                        )
                        .fetchFirst()
        );
    }

    public record RoomAccess(
            boolean owner,
            boolean participant,
            boolean banned,
            ConversationType type
    ) {
    }
}
