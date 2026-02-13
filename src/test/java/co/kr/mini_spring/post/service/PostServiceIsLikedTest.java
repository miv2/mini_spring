package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostLike;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.dto.response.PostResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostServiceIsLikedTest {

    @Test
    void getPost_sets_isLiked_true_when_like_exists() {
        PostRepository postRepository = mock(PostRepository.class);
        PostQueryRepository postQueryRepository = mock(PostQueryRepository.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
        co.kr.mini_spring.member.domain.repository.SocialMemberRepository socialMemberRepository = mock(co.kr.mini_spring.member.domain.repository.SocialMemberRepository.class);
        HashtagService hashtagService = mock(HashtagService.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        co.kr.mini_spring.post.cache.PostCacheService postCacheService = mock(co.kr.mini_spring.post.cache.PostCacheService.class);

        PostService postService = new PostService(
                postRepository,
                postQueryRepository,
                postLikeRepository,
                socialMemberRepository,
                hashtagService,
                redisTemplate,
                postCacheService
        );

        SocialMember user = SocialMember.builder()
                .id(1L)
                .email("test@example.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("tester")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        Post post = Post.builder().id(10L).authorId(2L).title("t").content("c").build();
        when(postQueryRepository.findByIdWithAllRelations(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findLike(1L, 10L)).thenReturn(Optional.of(mock(PostLike.class)));

        PostResponse response = postService.getPost(10L, user);

        assertThat(response.isLiked()).isTrue();
    }

    @Test
    void getPost_sets_isLiked_false_when_like_absent() {
        PostRepository postRepository = mock(PostRepository.class);
        PostQueryRepository postQueryRepository = mock(PostQueryRepository.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
        co.kr.mini_spring.member.domain.repository.SocialMemberRepository socialMemberRepository = mock(co.kr.mini_spring.member.domain.repository.SocialMemberRepository.class);
        HashtagService hashtagService = mock(HashtagService.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        co.kr.mini_spring.post.cache.PostCacheService postCacheService = mock(co.kr.mini_spring.post.cache.PostCacheService.class);

        PostService postService = new PostService(
                postRepository,
                postQueryRepository,
                postLikeRepository,
                socialMemberRepository,
                hashtagService,
                redisTemplate,
                postCacheService
        );

        SocialMember user = SocialMember.builder()
                .id(1L)
                .email("test@example.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("tester")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        Post post = Post.builder().id(10L).authorId(2L).title("t").content("c").build();
        when(postQueryRepository.findByIdWithAllRelations(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findLike(1L, 10L)).thenReturn(Optional.empty());

        PostResponse response = postService.getPost(10L, user);

        assertThat(response.isLiked()).isFalse();
    }
}
