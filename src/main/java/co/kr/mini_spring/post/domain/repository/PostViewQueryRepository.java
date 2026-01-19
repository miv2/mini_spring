package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.PostView;
import co.kr.mini_spring.post.domain.QPostView;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PostView 도메인 전용 Querydsl 리포지토리
 * - 게시글 조회수 중복 방지를 위한 복합 키 조회 및 락 제어를 담당합니다.
 */
@Repository
@RequiredArgsConstructor
public class PostViewQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QPostView postView = QPostView.postView;

    /**
     * 특정 사용자가 특정 게시글을 조회한 이력을 비관적 락을 걸어 조회합니다.
     * - 조회수 증가 로직의 원자성을 보장하기 위해 사용합니다.
     */
    public Optional<PostView> findByMemberIdAndPostIdWithLock(Long memberId, Long postId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(postView)
                        .where(
                                postView.id.memberId.eq(memberId),
                                postView.id.postId.eq(postId)
                        )
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}