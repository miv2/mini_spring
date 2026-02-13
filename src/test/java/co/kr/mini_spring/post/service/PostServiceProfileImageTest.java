package co.kr.mini_spring.post.service;

import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.dto.response.PostResponse;
import co.kr.mini_spring.post.dto.response.PostSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostServiceProfileImageTest {

    @Test
    void list_includes_profileImageUrl_with_default_when_missing() {
        PostRepository postRepository = mock(PostRepository.class);
        PostQueryRepository postQueryRepository = mock(PostQueryRepository.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
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
                hashtagService,
                redisTemplate,
                postCacheService
        );
        ReflectionTestUtils.setField(postService, "defaultProfileImage", "/uploads/default-profile.png");

        Post post = Post.builder().id(1L).authorId(2L).title("t").content("c").build();
        Pageable pageable = PageRequest.of(0, 10);
        Post spyPost = spy(post);
        when(postQueryRepository.findAllByPublishedPage(true, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(spyPost), pageable, 1));

        SocialMember author = SocialMember.builder()
                .id(2L)
                .email("a@a.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("n")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        // author has no profile image -> default
        doReturn(author).when(spyPost).getAuthor();

        PageResponse<PostSummaryResponse> response = postService.getPublishedPosts(pageable, null, null, null);
        assertThat(response.getContent().get(0).getProfileImageUrl()).isEqualTo("/uploads/default-profile.png");
    }

    @Test
    void detail_includes_profileImageUrl_with_default_when_missing() {
        PostRepository postRepository = mock(PostRepository.class);
        PostQueryRepository postQueryRepository = mock(PostQueryRepository.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
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
                hashtagService,
                redisTemplate,
                postCacheService
        );
        ReflectionTestUtils.setField(postService, "defaultProfileImage", "/uploads/default-profile.png");

        SocialMember user = SocialMember.builder()
                .id(1L)
                .email("u@u.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("u")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        SocialMember author = SocialMember.builder()
                .id(2L)
                .email("a@a.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("n")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        Post post = Post.builder().id(10L).authorId(2L).title("t").content("c").build();
        Post spyPost = spy(post);
        doReturn(author).when(spyPost).getAuthor();
        when(postQueryRepository.findByIdWithAllRelations(10L)).thenReturn(Optional.of(spyPost));

        when(postLikeRepository.findLike(1L, 10L)).thenReturn(Optional.empty());

        PostResponse response = postService.getPost(10L, user);
        assertThat(response.getProfileImageUrl()).isEqualTo("/uploads/default-profile.png");
    }
}
