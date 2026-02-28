package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberQueryRepository;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostLike;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private SocialMemberRepository socialMemberRepository;

    @Mock
    private SocialMemberQueryRepository socialMemberQueryRepository;

    @Mock
    private HashtagService hashtagService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "publicBaseUrl", "https://example.com");
        ReflectionTestUtils.setField(postService, "defaultProfileImage", "/uploads/default-profile.png");
    }

    @Test
    void addLike는_중복이어도_락을_먼저_획득한다() {
        Long memberId = 1L;
        Long postId = 2L;

        Post post = Post.builder()
                .id(postId)
                .authorId(999L)
                .likeCount(0)
                .build();

        when(postLikeRepository.findById(new PostLike.PostLikeId(memberId, postId)))
                .thenReturn(Optional.of(PostLike.builder()
                        .id(new PostLike.PostLikeId(memberId, postId))
                        .post(post)
                        .build()));
        lenient().when(postQueryRepository.findByIdWithPessimisticLock(postId))
                .thenReturn(Optional.of(post));

        postService.addLike(postId, memberId);

        verify(postQueryRepository, times(1)).findByIdWithPessimisticLock(postId);
        verify(postLikeRepository, times(1)).findById(new PostLike.PostLikeId(memberId, postId));
        verify(postLikeRepository, never()).save(any(PostLike.class));
        assertThat(post.getLikeCount()).isEqualTo(0);
    }

    @Test
    void removeLike는_락을_먼저_획득한_후_좋아요를_확인한다() {
        Long memberId = 1L;
        Long postId = 2L;
        Post post = Post.builder()
                .id(postId)
                .authorId(999L)
                .likeCount(1)
                .build();
        PostLike postLike = PostLike.builder()
                .id(new PostLike.PostLikeId(memberId, postId))
                .post(post)
                .build();

        when(postQueryRepository.findByIdWithPessimisticLock(postId))
                .thenReturn(Optional.of(post));
        when(postLikeRepository.findById(new PostLike.PostLikeId(memberId, postId)))
                .thenReturn(Optional.of(postLike));

        postService.removeLike(postId, memberId);

        InOrder inOrder = inOrder(postQueryRepository, postLikeRepository);
        inOrder.verify(postQueryRepository).findByIdWithPessimisticLock(postId);
        inOrder.verify(postLikeRepository).findById(new PostLike.PostLikeId(memberId, postId));
    }

    @Test
    void 캐시_키는_해시태그_순서와_무관해야한다() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        String key1 = (String) ReflectionTestUtils.invokeMethod(
                postService,
                "buildPostsCacheKey",
                pageable,
                "keyword",
                List.of("spring", "java"),
                7L
        );
        String key2 = (String) ReflectionTestUtils.invokeMethod(
                postService,
                "buildPostsCacheKey",
                pageable,
                "keyword",
                List.of("java", "spring"),
                7L
        );

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void 조회수_중복_방지는_setIfAbsent로_원자적으로_처리한다() {
        Long memberId = 10L;
        Long postId = 20L;

        SocialMember member = SocialMember.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);

        Post post = Post.builder()
                .id(postId)
                .authorId(999L)
                .build();

        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(any(String.class), any(), any(Duration.class)))
                .thenReturn(true);

        boolean result = (boolean) ReflectionTestUtils.invokeMethod(postService, "updateViewCount", post, member);

        assertThat(result).isTrue();
        verify(valueOps).setIfAbsent(
                "post:view:member:" + memberId + ":post:" + postId,
                "viewed",
                Duration.ofHours(1)
        );
        verify(postQueryRepository).incrementViewCount(postId);
    }
}
