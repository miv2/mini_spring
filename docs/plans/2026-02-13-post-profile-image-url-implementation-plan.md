# Post Profile Image URL Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add `profileImageUrl` to post list and detail responses, falling back to the configured default image.

**Architecture:** Inject default profile image path into `PostService`, compute URL via `author.getProfileImageUrl(defaultProfileImage)` (or default when author is null), and include it in `PostSummaryResponse` and `PostResponse` fields.

**Tech Stack:** Spring Boot 3, JUnit 5, Mockito.

---

### Task 1: Add failing unit tests for profileImageUrl

**Files:**
- Create: `/Users/miv/mini_spring/.worktrees/post-profile-image/src/test/java/co/kr/mini_spring/post/service/PostServiceProfileImageTest.java`

**Step 1: Write the failing test**

```java
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
        when(postQueryRepository.findAllByPublishedPage(true, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));

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
        when(post.getAuthor()).thenReturn(author);

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

        Post post = Post.builder().id(10L).authorId(2L).title("t").content("c").build();
        when(postQueryRepository.findByIdWithAllRelations(10L)).thenReturn(Optional.of(post));

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

        when(post.getAuthor()).thenReturn(author);
        when(postLikeRepository.findLike(1L, 10L)).thenReturn(Optional.empty());

        PostResponse response = postService.getPost(10L, user);
        assertThat(response.getProfileImageUrl()).isEqualTo("/uploads/default-profile.png");
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests PostServiceProfileImageTest`
Expected: FAIL (profileImageUrl not present)

**Step 3: Implement minimal code to pass**

- Add `profileImageUrl` field to `PostSummaryResponse` and `PostResponse`
- Inject `defaultProfileImage` into `PostService`
- Set `profileImageUrl` from `author.getProfileImageUrl(defaultProfileImage)` or default when author is null

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests PostServiceProfileImageTest`
Expected: PASS

**Step 5: Commit**

```bash
git add src/test/java/co/kr/mini_spring/post/service/PostServiceProfileImageTest.java \
  src/main/java/co/kr/mini_spring/post/service/PostService.java \
  src/main/java/co/kr/mini_spring/post/dto/response/PostSummaryResponse.java \
  src/main/java/co/kr/mini_spring/post/dto/response/PostResponse.java

git commit -m "feat: 게시글 프로필 이미지 URL 추가"
```

---

### Task 2: Verification

**Step 1: Run full test suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL
