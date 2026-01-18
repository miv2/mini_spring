package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostLike;
import co.kr.mini_spring.post.domain.PostView;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.domain.repository.PostViewRepository;
import co.kr.mini_spring.post.dto.request.PostCreateRequest;
import co.kr.mini_spring.post.dto.request.PostUpdateRequest;
import co.kr.mini_spring.post.dto.response.PostResponse;
import co.kr.mini_spring.post.dto.response.PostSummaryResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostViewRepository postViewRepository;
    private final MemberRepository memberRepository;
    private final HashtagService hashtagService;

    private static final Duration VIEW_COUNT_INTERVAL = Duration.ofHours(1);

    // 게시글을 작성하고 요청된 해시태그를 연결합니다.
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Member member) {
        requireAuthenticated(member);

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .member(member)
                .build();

        postRepository.save(post);
        hashtagService.attachHashtagsToPost(post, request.getHashtags());

        return new PostResponse(post, member);
    }

    // 게시글 상세 조회 시 조회수 증가 규칙을 적용합니다.
    @Transactional
    public PostResponse getPost(Long postId, Member currentUser) {
        Post post = findPostWithRelationsOrThrow(postId);
        if (post.getMember() == null) {
            throw new BusinessException(ResponseCode.POST_NOT_FOUND);
        }
        Integer viewCountOverride = null;
        if (currentUser != null && updateViewCount(post, currentUser)) {
            viewCountOverride = postRepository.findViewCountById(post.getId());
        }
        return new PostResponse(post, currentUser, viewCountOverride);
    }

    // 작성자가 게시글을 수정하고 해시태그를 갱신합니다.
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request, Member member) {
        requireAuthenticated(member);

        Post post = findPostWithRelationsOrThrow(postId);
        requireOwnership(post, member, ResponseCode.NO_PERMISSION_TO_UPDATE_POST);

        post.update(request.getTitle(), request.getContent());
        hashtagService.updateHashtagsForPost(post, request.getHashtags());

        return new PostResponse(post, member);
    }

    // 작성자가 본인의 게시글을 삭제합니다.
    @Transactional
    public void deletePost(Long postId, Member member) {
        requireAuthenticated(member);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        requireOwnership(post, member, ResponseCode.NO_PERMISSION_TO_DELETE_POST);

        postRepository.delete(post);
    }

    /**
     * 게시글에 좋아요를 추가합니다.
     * 이미 좋아요한 경우 예외가 발생하지 않고 무시됩니다.
     */
    @Transactional
    public void addLike(Long postId, Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }

        // 이미 좋아요했는지 확인
        if (postLikeRepository.findByMemberIdAndPostId(memberId, postId).isPresent()) {
            return; // 이미 좋아요한 경우 무시
        }

        Post post = postRepository.findByIdWithPessimisticLock(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

        Member memberProxy = memberRepository.getReferenceById(memberId);
        PostLike newLike = PostLike.builder()
                .id(new PostLike.PostLikeId(memberId, postId))
                .member(memberProxy)
                .post(post)
                .build();
        postLikeRepository.save(newLike);
        post.increaseLikeCount();
    }

    /**
     * 게시글의 좋아요를 취소합니다.
     * 좋아요하지 않은 경우 예외가 발생하지 않고 무시됩니다.
     */
    @Transactional
    public void removeLike(Long postId, Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }

        PostLike postLike = postLikeRepository.findByMemberIdAndPostId(memberId, postId)
                .orElse(null);

        if (postLike == null) {
            return; // 좋아요하지 않은 경우 무시
        }

        Post post = postRepository.findByIdWithPessimisticLock(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

        postLikeRepository.delete(postLike);
        post.decreaseLikeCount();
    }

    public PageResponse<PostSummaryResponse> getPublishedPosts(Pageable pageable, String keyword, List<String> hashtags, Long authorId) {
        // 공개된 게시글만 조회하고 요약 DTO로 변환합니다.
        Page<Post> postPage = postRepository.findAllByPublished(true, pageable, keyword, hashtags, authorId);
        Page<PostSummaryResponse> dtoPage = postPage.map(PostSummaryResponse::new);
        return new PageResponse<>(dtoPage);
    }

    /**
     * 동일 회원의 1시간 내 재조회는 조회수 증가 없이 마지막 조회 시각만 갱신합니다.
     * 최초 조회거나 1시간이 지난 경우에만 조회수를 증가시킵니다.
     */
    // 조회수 증가 여부를 판단하고 필요 시 뷰 카운트를 올립니다.
    private boolean updateViewCount(Post post, Member currentUser) {
        Long memberId = currentUser.getId();
        if (memberId == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        PostView postView = postViewRepository.findByMemberIdAndPostIdWithLock(memberId, post.getId())
                .orElse(null);

        if (postView == null) {
            PostView newView = PostView.builder()
                    .id(new PostView.PostViewId(memberId, post.getId()))
                    .member(memberRepository.getReferenceById(memberId))
                    .post(postRepository.getReferenceById(post.getId()))
                    .lastViewedAt(now)
                    .build();
            try {
                postViewRepository.save(newView);
                incrementViewCount(post.getId());
                return true;
            } catch (DataIntegrityViolationException ignored) {
                // 동일 시점 중복 저장을 무시하고 조회수 증가도 생략합니다.
            }
            return false;
        }

        if (postView.isViewCountable(now, VIEW_COUNT_INTERVAL)) {
            postView.updateLastViewedAt(now);
            postView.increaseViewCount();
            incrementViewCount(post.getId());
            return true;
        }
        return false;
    }

    // DB의 조회수 카운트를 원자적으로 증가시킵니다.
    private void incrementViewCount(Long postId) {
        postRepository.incrementViewCount(postId);
    }

    private void requireAuthenticated(Member member) {
        if (member == null) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }
    }

    // 게시글 소유자만 접근 가능한 작업에 대한 권한 검증을 수행합니다.
    private void requireOwnership(Post post, Member member, ResponseCode noPermissionCode) {
        if (post.getMember() == null || member == null || !Objects.equals(post.getMember().getId(), member.getId())) {
            throw new BusinessException(noPermissionCode);
        }
    }

    // 게시글과 연관 데이터를 모두 로딩하며 존재하지 않으면 예외를 던집니다.
    private Post findPostWithRelationsOrThrow(Long postId) {
        return postRepository.findByIdWithAllRelations(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
    }
}
