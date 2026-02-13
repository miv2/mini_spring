package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.dto.request.PostCreateRequest;
import co.kr.mini_spring.post.dto.response.PostResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostServiceCreatePostProfileImageTest {

    @Test
    void createPost_uses_author_with_profile_image_loaded() {
        PostRepository postRepository = mock(PostRepository.class);
        PostQueryRepository postQueryRepository = mock(PostQueryRepository.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
        SocialMemberRepository socialMemberRepository = mock(SocialMemberRepository.class);
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
        ReflectionTestUtils.setField(postService, "defaultProfileImage", "/uploads/default-profile.png");

        SocialMember member = SocialMember.builder()
                .id(1L)
                .email("user@example.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("u")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        SocialMember memberWithProfile = SocialMember.builder()
                .id(1L)
                .email("user@example.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("u")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(socialMemberRepository.findByEmailWithProfileImage("user@example.com"))
                .thenReturn(Optional.of(memberWithProfile));

        PostCreateRequest request = mock(PostCreateRequest.class);
        when(request.getTitle()).thenReturn("t");
        when(request.getContent()).thenReturn("c");

        PostResponse response = postService.createPost(request, member);

        assertThat(response.getProfileImageUrl()).isNotBlank();
    }
}
