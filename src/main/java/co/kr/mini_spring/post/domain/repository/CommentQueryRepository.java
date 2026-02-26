package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.Comment;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static co.kr.mini_spring.post.domain.QComment.comment;

/**
 * Comment 도메인 전용 Querydsl 리포지토리
 * - 계층형 댓글 조회 최적화 및 N+1 문제 해결을 담당합니다.
 */
@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 게시글의 최상위 댓글 목록을 오프셋 기반 페이징으로 조회합니다.
     * - 부모가 없는(parent is null) 최상위 댓글만 조회합니다.
     * - 대댓글은 엔티티의 @BatchSize 설정을 통해 효율적으로 가져옵니다.
     */
    public Page<Comment> findAllTopLevelCommentsByPostIdPage(Long postId, Pageable pageable) {
        List<Comment> content = queryFactory
                .selectFrom(comment)
                .where(
                        comment.post.id.eq(postId),
                        comment.parent.isNull())
                .orderBy(comment.createdAt.desc(), comment.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(
                        comment.post.id.eq(postId),
                        comment.parent.isNull())
                .fetchOne();

        long safeTotal = (total == null) ? 0L : total;
        return new PageImpl<>(content, pageable, safeTotal);
    }

    /**
     * 댓글 ID로 상세 정보를 조회하며 작성자 정보를 함께 가져옵니다.
     * - 수정/삭제 권한 체크 시 N+1 문제를 방지하기 위해 사용합니다.
     */
    public Optional<Comment> findByIdWithMember(Long id) {
        return Optional.ofNullable(
                queryFactory.selectFrom(comment)
                        .where(comment.id.eq(id))
                        .fetchOne());
    }
}
