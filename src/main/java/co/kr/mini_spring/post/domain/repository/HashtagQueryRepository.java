package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.QHashtag;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hashtag 도메인 전용 Querydsl 리포지토리
 * - 대량의 해시태그 업데이트 성능 최적화를 위한 벌크 연산을 담당합니다.
 */
@Repository
@RequiredArgsConstructor
public class HashtagQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QHashtag hashtag = QHashtag.hashtag;

    /**
     * 여러 해시태그의 사용 횟수를 한 번의 쿼리로 일괄 증가시킵니다.
     * - 루프를 도는 대신 벌크 업데이트를 사용하여 데이터베이스 부하를 줄입니다.
     */
    public long bulkIncreaseUsageCount(List<String> names) {
        return queryFactory.update(hashtag)
                .set(hashtag.usageCount, hashtag.usageCount.add(1))
                .set(hashtag.lastUsedAt, LocalDateTime.now())
                .where(hashtag.name.in(names))
                .execute();
    }
}