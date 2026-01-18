package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PostLikeConcurrencyTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long postId;
    private final List<Long> memberIds = new ArrayList<>();
    private final int THREAD_COUNT = 100;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 게시글 생성
        Post post = Post.builder()
                .title("동시성 테스트 제목")
                .content("동시성 테스트 내용")
                .build();
        postId = postRepository.save(post).getId();

        // 2. 테스트용 회원 100명 생성
        for (int i = 0; i < THREAD_COUNT; i++) {
            Member member = Member.builder()
                    .email("user" + i + "@test.com")
                    .name("User" + i)
                    .nickname("Nickname" + i)
                    .build();
            memberIds.add(memberRepository.save(member).getId());
        }
    }

    @Autowired
    private co.kr.mini_spring.post.domain.repository.PostLikeRepository postLikeRepository;

    @AfterEach
    void tearDown() {
        // 1. 좋아요 이력 먼저 삭제 (Post, Member를 참조하므로)
        postLikeRepository.deleteAllInBatch();
        
        // 2. 게시글 삭제
        postRepository.deleteById(postId);
        
        // 3. 테스트용 회원들 삭제
        memberRepository.deleteAllByIdInBatch(memberIds);
        memberIds.clear();
    }

    @Test
    @DisplayName("좋아요 동시성 테스트 - 100명의 사용자가 동시에 좋아요를 누르면 카운트가 100이 되어야 한다")
    void concurrency_like_test() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final Long memberId = memberIds.get(i);
            executorService.submit(() -> {
                try {
                    postService.addLike(postId, memberId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드의 작업이 끝날 때까지 대기

        // then
        Post post = postRepository.findById(postId).orElseThrow();
        assertThat(post.getLikeCount()).isEqualTo(THREAD_COUNT);
    }
}
