package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.member.domain.QMember;
import co.kr.mini_spring.post.domain.Comment;
import co.kr.mini_spring.post.domain.QComment;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Comment 도메인 전용 Querydsl 리포지토리
 * - 계층형 댓글 조회 최적화 및 N+1 문제 해결을 담당합니다.
 */
@Repository
@RequiredArgsConstructor
public class CommentQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QComment comment = QComment.comment;
    private static final QMember member = QMember.member;

    /**
     * 특정 게시글의 최상위 댓글 목록을 페이징 조회합니다.
     * - 작성자 정보(Member)를 Fetch Join하여 회원 정보를 가져올 때 발생하는 N+1 문제를 방지합니다.
     * - 대댓글(children)은 엔티티 설정에 따라 지연 로딩됩니다.
     */
    public Page<Comment> findAllTopLevelCommentsByPostId(Long postId, Pageable pageable) {
        List<Comment> content = queryFactory
                .selectFrom(comment)
                .leftJoin(comment.member, member).fetchJoin()
                .where(
                        comment.post.id.eq(postId),
                        comment.parent.isNull() // 최상위 댓글만 필터링
                )
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(
                        comment.post.id.eq(postId),
                        comment.parent.isNull()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /**
     * 댓글 ID로 상세 정보를 조회하며 작성자 정보를 함께 가져옵니다.
     * - 수정/삭제 권한 체크 시 N+1 문제를 방지하기 위해 사용합니다.
     */
    public Optional<Comment> findByIdWithMember(Long id) {
        return Optional.ofNullable(
                queryFactory.selectFrom(comment)
                        .leftJoin(comment.member, member).fetchJoin()
                        .where(comment.id.eq(id))
                        .fetchOne()
        );
    }
}