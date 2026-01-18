package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.member.domain.QMember;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.QHashtag;
import co.kr.mini_spring.post.domain.QPost;
import co.kr.mini_spring.post.domain.QPostHashtag;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private static final QPost post = QPost.post;
    private static final QPostHashtag postHashtag = QPostHashtag.postHashtag;
    private static final QHashtag hashtag = QHashtag.hashtag;
    private static final QMember member = QMember.member;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Post> findByIdWithPessimisticLock(Long id) {
        Post result = queryFactory.selectFrom(post)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .where(post.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Post> findByIdWithAllRelations(Long id) {
        Post result = queryFactory.selectFrom(post)
                .leftJoin(post.member, member).fetchJoin()
                .leftJoin(post.postHashtags, postHashtag).fetchJoin()
                .leftJoin(postHashtag.hashtag, hashtag).fetchJoin()
                .where(post.id.eq(id))
                .distinct()
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Page<Post> findAllByPublished(boolean published, Pageable pageable, String keyword, List<String> hashtagsFilter, Long authorId) {
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(pageable);

        BooleanExpression conditions = post.published.eq(published)
                .and(post.member.isNotNull())
                .and(applyAuthor(authorId))
                .and(applyKeyword(keyword))
                .and(applyHashtagFilter(hashtagsFilter));

        List<Long> ids = queryFactory.select(post.id)
                .from(post)
                .leftJoin(post.member, member) // to-one만 join
                .where(conditions)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<Post> posts = ids.isEmpty() ? List.of() :
                queryFactory.selectFrom(post)
                        .leftJoin(post.member, member).fetchJoin()
                        .leftJoin(post.postHashtags, postHashtag).fetchJoin()
                        .leftJoin(postHashtag.hashtag, hashtag).fetchJoin()
                        .where(post.id.in(ids))
                        .distinct()
                        .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                        .fetch();

        Long total = queryFactory.select(post.count())
                .from(post)
                .where(conditions)
                .fetchOne();

        return new PageImpl<>(posts, pageable, total == null ? 0 : total);
    }

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
                    }
            );
        });

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(post.createdAt.desc());
        }

        return orderSpecifiers;
    }

    private BooleanExpression applyAuthor(Long authorId) {
        if (authorId == null) {
            return null;
        }
        return post.member.id.eq(authorId);
    }

    private BooleanExpression applyKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String kw = keyword.trim();
        return post.title.containsIgnoreCase(kw)
                .or(post.content.containsIgnoreCase(kw));
    }

    private BooleanExpression applyHashtagFilter(List<String> hashtagsFilter) {
        if (hashtagsFilter == null || hashtagsFilter.isEmpty()) {
            return null;
        }
        // 해시태그 이름이 모두 포함된 게시글을 필터링
        Set<String> names = Set.copyOf(hashtagsFilter);
        return post.id.in(
                queryFactory
                        .select(postHashtag.post.id)
                        .from(postHashtag)
                        .leftJoin(postHashtag.hashtag, hashtag)
                        .groupBy(postHashtag.post.id)
                        .having(hashtag.name.in(names))
                        .fetch()
        );
    }
}
