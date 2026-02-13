package co.kr.mini_spring.post.service;

import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.post.cache.PostCacheService;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.dto.response.PostSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PostServiceCacheTest {

    @Test
    void list_isCachedAfterFirstRead() {
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

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Post post = Post.builder()
                .id(1L)
                .title("title")
                .authorId(3L)
                .createdAt(LocalDateTime.now())
                .build();
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);

        SocialMember author = SocialMember.builder()
                .id(3L)
                .email("test@example.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("tester")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        when(postQueryRepository.findAllByPublishedPage(anyBoolean(), any(), any(), any(), any()))
                .thenReturn(page);
        when(socialMemberRepository.findAllById(Set.of(3L))).thenReturn(List.of(author));

        PageResponse<PostSummaryResponse> cachedResponse = new PageResponse<>(
                List.of(new PostSummaryResponse(post, author)),
                0, 10, 1, 1, false
        );

        when(cacheService.get(anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(cachedResponse));

        PageResponse<PostSummaryResponse> first = postService.getPublishedPosts(pageable, null, null, null);
        PageResponse<PostSummaryResponse> second = postService.getPublishedPosts(pageable, null, null, null);

        assertThat(second).isSameAs(cachedResponse);
        assertThat(first.getContent()).hasSize(1);

        verify(postQueryRepository, times(1))
                .findAllByPublishedPage(anyBoolean(), any(), any(), any(), any());
        verify(cacheService, times(1)).putList(anyString(), any());
        verify(cacheService, times(2)).get(anyString());
    }
}
