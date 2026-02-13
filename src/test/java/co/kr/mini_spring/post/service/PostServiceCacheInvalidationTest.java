package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.post.cache.PostCacheService;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostLike;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.mockito.Mockito.*;

class PostServiceCacheInvalidationTest {

    @Test
    void deletePost_evictsLists() {
        PostRepository postRepository = mock(PostRepository.class);
        PostQueryRepository postQueryRepository = mock(PostQueryRepository.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
        SocialMemberRepository socialMemberRepository = mock(SocialMemberRepository.class);
        HashtagService hashtagService = mock(HashtagService.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        PostCacheService cacheService = mock(PostCacheService.class);

        PostService postService = new PostService(
                postRepository,
                postQueryRepository,
                postLikeRepository,
                socialMemberRepository,
                hashtagService,
                redisTemplate,
                cacheService
        );

        SocialMember member = SocialMember.builder()
                .id(1L)
                .email("test@example.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("tester")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        Post post = Post.builder().id(10L).authorId(1L).title("t").content("c").build();
        when(postQueryRepository.findByIdWithMember(10L)).thenReturn(Optional.of(post));

        postService.deletePost(10L, member);

        verify(cacheService).evictLists();
    }

    @Test
    void addLike_evictsLists() {
        PostRepository postRepository = mock(PostRepository.class);
        PostQueryRepository postQueryRepository = mock(PostQueryRepository.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
        SocialMemberRepository socialMemberRepository = mock(SocialMemberRepository.class);
        HashtagService hashtagService = mock(HashtagService.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        PostCacheService cacheService = mock(PostCacheService.class);

        PostService postService = new PostService(
                postRepository,
                postQueryRepository,
                postLikeRepository,
                socialMemberRepository,
                hashtagService,
                redisTemplate,
                cacheService
        );

        Post post = Post.builder().id(12L).authorId(2L).title("t").content("c").build();
        when(postLikeRepository.findLike(3L, 12L)).thenReturn(Optional.empty());
        when(postQueryRepository.findByIdWithPessimisticLock(12L)).thenReturn(Optional.of(post));

        postService.addLike(12L, 3L);

        verify(cacheService).evictLists();
    }
}
