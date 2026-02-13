package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.QHashtag;
import co.kr.mini_spring.post.domain.QPost;
import co.kr.mini_spring.post.domain.QPostHashtag;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Post 도메인 전용 Querydsl 리포지토리
 * - 복잡한 동적 쿼리, Fetch Join을 통한 성능 최적화, 벌크 업데이트를 담당합니다.
 */
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QPost post = QPost.post;
    private static final QPostHashtag postHashtag = QPostHashtag.postHashtag;
    private static final QHashtag hashtag = QHashtag.hashtag;

    /**
     * 게시글을 비관적 락(PESSIMISTIC_WRITE)을 걸어 조회합니다.
     * - 동시성 제어가 필요한 조회수/좋아요 등의 업데이트 시 사용합니다.
     */
    public Optional<Post> findByIdWithPessimisticLock(Long id) {
        return Optional.ofNullable(
                queryFactory.selectFrom(post)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .where(post.id.eq(id))
                        .fetchOne());
    }

    /**
     * 게시글의 모든 연관관계(작성자, 해시태그)를 Fetch Join하여 한 번에 조회합니다.
     * - N+1 문제를 방지하기 위해 상세 페이지 조회 시 사용합니다.
     */
    public Optional<Post> findByIdWithAllRelations(Long id) {
        return Optional.ofNullable(
                queryFactory.selectFrom(post)
                        .leftJoin(post.author).fetchJoin()
                        .leftJoin(post.postHashtags, postHashtag).fetchJoin()
                        .leftJoin(postHashtag.hashtag, hashtag).fetchJoin()
                        .where(post.id.eq(id))
                        .distinct()
                        .fetchOne());
    }

    /**
     * 게시글 목록을 오프셋 기반 페이징으로 조회합니다.
     * - Pageable을 통해 offset, limit, sort를 처리합니다.
     * - 성능을 위해 ID를 먼저 조회한 뒤 실데이터를 Fetch Join으로 가져옵니다.
     */
    public Page<Post> findAllByPublishedPage(boolean published, String keyword,
                                            List<String> hashtagsFilter, Long authorId, Pageable pageable) {
        BooleanExpression conditions = post.published.eq(published)
                .and(post.authorId.isNotNull())
                .and(applyAuthor(authorId))
                .and(applyKeyword(keyword))
                .and(applyHashtagFilter(hashtagsFilter));

        // 1. 전체 카운트 조회
        Long total = queryFactory.select(post.count())
                .from(post)
                .where(conditions)
                .fetchOne();

        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 2. 페이징에 맞는 ID 목록 조회
        List<Long> ids = queryFactory.select(post.id)
                .from(post)
                .where(conditions)
                .orderBy(buildOrderSpecifiers(pageable).toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3. 실데이터 Fetch Join 조회
        List<Post> content = queryFactory.selectFrom(post)
                .leftJoin(post.author).fetchJoin()
                .leftJoin(post.postHashtags, postHashtag).fetchJoin()
                .leftJoin(postHashtag.hashtag, hashtag).fetchJoin()
                .where(post.id.in(ids))
                .distinct()
                .orderBy(buildOrderSpecifiers(pageable).toArray(new OrderSpecifier[0]))
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 게시글과 작성자 정보만 Fetch Join하여 조회합니다.
     * - 수정/삭제 권한 체크와 같이 최소한의 연관 정보만 필요할 때 사용합니다.
     */
    public Optional<Post> findByIdWithMember(Long id) {
        return Optional.ofNullable(
                queryFactory.selectFrom(post)
                        .where(post.id.eq(id))
                        .fetchOne());
    }

    /**
     * 게시글의 조회수를 원자적으로 1 증가시킵니다.
     */
    public long incrementViewCount(Long id) {
        return queryFactory.update(post)
                .set(post.viewCount, post.viewCount.add(1))
                .where(post.id.eq(id))
                .execute();
    }

    /**
     * 게시글의 현재 조회수만 DB에서 직접 조회합니다.
     */
    public Integer findViewCountById(Long id) {
        return queryFactory.select(post.viewCount)
                .from(post)
                .where(post.id.eq(id))
                .fetchOne();
    }

    /**
     * 게시글의 댓글 수를 원자적으로 1 증가시킵니다.
     */
    public long incrementCommentCount(Long id) {
        return queryFactory.update(post)
                .set(post.commentCount, post.commentCount.add(1))
                .where(post.id.eq(id))
                .execute();
    }

    /**
     * 게시글의 댓글 수를 원자적으로 1 감소시킵니다. (0 이하 방지 로직 포함)
     */
    public long decrementCommentCount(Long id) {
        return queryFactory.update(post)
                .set(post.commentCount,
                        new CaseBuilder()
                                .when(post.commentCount.gt(0)).then(post.commentCount.subtract(1))
                                .otherwise(0))
                .where(post.id.eq(id))
                .execute();
    }

    /**
     * 정렬 조건을 동적으로 생성합니다.
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        pageable.getSort().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            orderSpecifiers.add(
                    switch (order.getProperty()) {
                        case "createdAt" -> new OrderSpecifier<>(direction, post.createdAt);
                        case "likeCount" -> new OrderSpecifier<>(direction, post.likeCount);
                        case "viewCount" -> new OrderSpecifier<>(direction, post.viewCount);
                        case "title" -> new OrderSpecifier<>(direction, post.title);
                        default -> new OrderSpecifier<>(direction, post.id);
                    });
        });
        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(post.createdAt.desc());
        }
        return orderSpecifiers;
    }

    /**
     * 작성자 필터링 조건을 생성합니다.
     */
    private BooleanExpression applyAuthor(Long authorId) {
        return authorId == null ? null : post.authorId.eq(authorId);
    }

    /**
     * 제목 및 내용 키워드 검색 조건을 생성합니다.
     */
    private BooleanExpression applyKeyword(String keyword) {
        if (keyword == null || keyword.isBlank())
            return null;
        String kw = keyword.trim();
        return post.title.containsIgnoreCase(kw).or(post.content.containsIgnoreCase(kw));
    }

    /**
     * 해시태그 필터링 조건을 생성합니다.
     */
    private BooleanExpression applyHashtagFilter(List<String> hashtagsFilter) {
        if (hashtagsFilter == null || hashtagsFilter.isEmpty())
            return null;
        Set<String> names = Set.copyOf(hashtagsFilter);
        return post.id.in(
                queryFactory.select(postHashtag.post.id)
                        .from(postHashtag)
                        .leftJoin(postHashtag.hashtag, hashtag)
                        .groupBy(postHashtag.post.id)
                        .having(hashtag.name.in(names))
                        .fetch());
    }
}
