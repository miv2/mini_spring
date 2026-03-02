package co.kr.mini_spring.chat.domain.repository;

import co.kr.mini_spring.chat.domain.Message;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static co.kr.mini_spring.chat.domain.QMessage.message;

@Repository
@RequiredArgsConstructor
public class ChatMessageQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Message> findMessagesByCursor(Long conversationId, Long cursor, int limit) {
        return queryFactory.selectFrom(message)
                .where(
                        message.conversationId.eq(conversationId),
                        message.deletedAt.isNull(),
                        cursorCondition(cursor)
                )
                .orderBy(message.id.desc())
                .limit(limit)
                .fetch();
    }

    public Optional<Long> findLatestMessageId(Long conversationId) {
        return Optional.ofNullable(
                queryFactory.select(message.id.max())
                        .from(message)
                        .where(
                                message.conversationId.eq(conversationId),
                                message.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }

    private BooleanExpression cursorCondition(Long cursor) {
        return cursor == null ? null : message.id.lt(cursor);
    }
}

