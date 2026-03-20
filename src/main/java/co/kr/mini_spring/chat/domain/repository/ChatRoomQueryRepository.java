package co.kr.mini_spring.chat.domain.repository;

import co.kr.mini_spring.chat.domain.ConversationType;
import co.kr.mini_spring.chat.domain.QConversation;
import co.kr.mini_spring.chat.domain.QConversationParticipant;
import co.kr.mini_spring.chat.dto.response.ChatRoomResponse;
import co.kr.mini_spring.member.domain.QSocialMember;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static co.kr.mini_spring.chat.domain.QConversation.conversation;
import static co.kr.mini_spring.chat.domain.QConversationBan.conversationBan;
import static co.kr.mini_spring.chat.domain.QMessage.message;

@Repository
@RequiredArgsConstructor
public class ChatRoomQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ChatRoomResponse> findMyRooms(Long userId, Long cursor, int limit) {
        var participant = new QConversationParticipant("cp_outer");
        Expression<Long> unreadCountExpr = unreadCountExpression(userId, participant);
        Expression<Long> participantCountExpr = participantCountExpression();
        Expression<String> titleExpr = titleExpression(userId);
        DateTimeExpression<LocalDateTime> sortAtExpr = conversation.lastMessageAt.coalesce(conversation.createdAt);

        List<Tuple> rows = queryFactory
                .select(
                        conversation.id,
                        conversation.type,
                        titleExpr,
                        conversation.ownerId,
                        conversation.lastMessagePreview,
                        conversation.lastMessageAt,
                        unreadCountExpr,
                        participantCountExpr
                )
                .from(participant)
                .join(conversation).on(participant.conversationId.eq(conversation.id))
                .where(
                        participant.userId.eq(userId),
                        participant.deletedAt.isNull(),
                        conversation.deletedAt.isNull(),
                        roomCursorCondition(cursor, sortAtExpr)
                )
                .orderBy(sortAtExpr.desc(), conversation.id.desc())
                .limit(limit)
                .fetch();

        return rows.stream()
                .map(tuple -> toRoomResponse(tuple, titleExpr, unreadCountExpr, participantCountExpr))
                .toList();
    }

    public List<ChatRoomResponse> findPublicGroupRooms(Long userId, long maxMembers, Long cursor, int limit) {
        Expression<Long> participantCountExpr = participantCountExpression();
        Expression<Long> unreadCountExpr = Expressions.constant(0L);
        Expression<String> titleExpr = conversation.title;
        DateTimeExpression<LocalDateTime> sortAtExpr = conversation.lastMessageAt.coalesce(conversation.createdAt);

        List<Tuple> rows = queryFactory
                .select(
                        conversation.id,
                        conversation.type,
                        titleExpr,
                        conversation.ownerId,
                        conversation.lastMessagePreview,
                        conversation.lastMessageAt,
                        unreadCountExpr,
                        participantCountExpr
                )
                .from(conversation)
                .where(
                        conversation.type.eq(ConversationType.GROUP),
                        conversation.deletedAt.isNull(),
                        roomCursorCondition(cursor, sortAtExpr),
                        maxMembersCondition(maxMembers),
                        JPAExpressions.selectOne()
                                .from(conversationBan)
                                .where(
                                        conversationBan.conversationId.eq(conversation.id),
                                        conversationBan.userId.eq(userId)
                                )
                                .notExists()
                )
                .orderBy(sortAtExpr.desc(), conversation.id.desc())
                .limit(limit)
                .fetch();

        return rows.stream()
                .map(tuple -> toRoomResponse(tuple, titleExpr, unreadCountExpr, participantCountExpr))
                .toList();
    }

    private ChatRoomResponse toRoomResponse(Tuple tuple,
                                            Expression<String> titleExpr,
                                            Expression<Long> unreadCountExpr,
                                            Expression<Long> participantCountExpr) {
        return new ChatRoomResponse(
                tuple.get(conversation.id),
                tuple.get(conversation.type),
                tuple.get(titleExpr),
                tuple.get(conversation.ownerId),
                tuple.get(conversation.lastMessagePreview),
                tuple.get(conversation.lastMessageAt),
                tuple.get(unreadCountExpr),
                tuple.get(participantCountExpr)
        );
    }

    private Expression<Long> participantCountExpression() {
        var participantCount = new QConversationParticipant("cp_count");
        return JPAExpressions.select(participantCount.count())
                .from(participantCount)
                .where(
                        participantCount.conversationId.eq(conversation.id),
                        participantCount.deletedAt.isNull()
                );
    }

    private Expression<Long> unreadCountExpression(Long userId,
                                                   QConversationParticipant participant) {
        return JPAExpressions.select(message.count())
                .from(message)
                .where(
                        message.conversationId.eq(conversation.id),
                        message.deletedAt.isNull(),
                        message.senderId.ne(userId),
                        message.id.gt(participant.lastReadMessageId.coalesce(0L))
                );
    }

    private Expression<String> titleExpression(Long userId) {
        var participantPeer = new QConversationParticipant("cp_peer");
        var peer = new QSocialMember("peer_member");
        return new CaseBuilder()
                .when(conversation.type.eq(ConversationType.GROUP))
                .then(conversation.title)
                .otherwise(
                        JPAExpressions.select(peer.nickname)
                                .from(participantPeer)
                                .join(peer).on(participantPeer.userId.eq(peer.id))
                                .where(
                                        participantPeer.conversationId.eq(conversation.id),
                                        participantPeer.userId.ne(userId),
                                        participantPeer.deletedAt.isNull()
                                )
                );
    }

    private BooleanExpression maxMembersCondition(long maxMembers) {
        var participantLimit = new co.kr.mini_spring.chat.domain.QConversationParticipant("cp_limit");
        return JPAExpressions.selectOne()
                .from(participantLimit)
                .where(
                        participantLimit.conversationId.eq(conversation.id),
                        participantLimit.deletedAt.isNull()
                )
                .groupBy(participantLimit.conversationId)
                .having(participantLimit.count().loe(maxMembers))
                .exists();
    }

    private BooleanExpression roomCursorCondition(Long cursor, DateTimeExpression<LocalDateTime> sortAtExpr) {
        if (cursor == null) {
            return null;
        }
        var cursorConversation = new QConversation("cursor_conversation");
        DateTimeExpression<LocalDateTime> cursorSortAt = cursorConversation.lastMessageAt.coalesce(cursorConversation.createdAt);
        var cursorSortAtSubQuery = JPAExpressions.select(cursorSortAt)
                .from(cursorConversation)
                .where(
                        cursorConversation.id.eq(cursor),
                        cursorConversation.deletedAt.isNull()
                );
        return sortAtExpr.lt(cursorSortAtSubQuery)
                .or(
                        sortAtExpr.eq(cursorSortAtSubQuery)
                                .and(conversation.id.lt(cursor))
                );
    }

}
