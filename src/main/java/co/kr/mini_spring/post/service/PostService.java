package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostLike;
import co.kr.mini_spring.post.domain.PostView;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostViewQueryRepository;
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
    private final PostQueryRepository postQueryRepository; // 추가
    private final PostLikeRepository postLikeRepository;
    private final PostViewQueryRepository postViewQueryRepository; // 변경
    private final MemberRepository memberRepository;
    private final HashtagService hashtagService;

    private static final Duration VIEW_COUNT_INTERVAL = Duration.ofHours(1);

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

    @Transactional
    public PostResponse getPost(Long postId, Member currentUser) {
        Post post = postQueryRepository.findByIdWithAllRelations(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        if (post.getMember() == null) {
            throw new BusinessException(ResponseCode.POST_NOT_FOUND);
        }
        Integer viewCountOverride = null;
        if (currentUser != null && updateViewCount(post, currentUser)) {
            viewCountOverride = postQueryRepository.findViewCountById(post.getId());
        }
        return new PostResponse(post, currentUser, viewCountOverride);
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request, Member member) {
        requireAuthenticated(member);
        Post post = postQueryRepository.findByIdWithAllRelations(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        requireOwnership(post, member, ResponseCode.NO_PERMISSION_TO_UPDATE_POST);
        post.update(request.getTitle(), request.getContent());
        hashtagService.updateHashtagsForPost(post, request.getHashtags());
        return new PostResponse(post, member);
    }

    @Transactional
    public void deletePost(Long postId, Member member) {
        requireAuthenticated(member);
        Post post = postQueryRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        requireOwnership(post, member, ResponseCode.NO_PERMISSION_TO_DELETE_POST);
        post.delete();
    }

    @Transactional
    public void addLike(Long postId, Long memberId) {
        if (memberId == null) throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        if (postLikeRepository.findByMemberIdAndPostId(memberId, postId).isPresent()) return;
        
        Post post = postQueryRepository.findByIdWithPessimisticLock(postId)
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

    @Transactional
    public void removeLike(Long postId, Long memberId) {
        if (memberId == null) throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        PostLike postLike = postLikeRepository.findByMemberIdAndPostId(memberId, postId).orElse(null);
        if (postLike == null) return;

        Post post = postQueryRepository.findByIdWithPessimisticLock(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        postLikeRepository.delete(postLike);
        post.decreaseLikeCount();
    }

    public PageResponse<PostSummaryResponse> getPublishedPosts(Pageable pageable, String keyword, List<String> hashtags, Long authorId) {
        Page<Post> postPage = postQueryRepository.findAllByPublished(true, pageable, keyword, hashtags, authorId);
        return new PageResponse<>(postPage.map(PostSummaryResponse::new));
    }

    private boolean updateViewCount(Post post, Member currentUser) {
        Long memberId = currentUser.getId();
        if (memberId == null) return false;

        LocalDateTime now = LocalDateTime.now();
        PostView postView = postViewQueryRepository.findByMemberIdAndPostIdWithLock(memberId, post.getId()).orElse(null);

        if (postView == null) {
            PostView newView = PostView.builder()
                    .id(new PostView.PostViewId(memberId, post.getId()))
                    .member(memberRepository.getReferenceById(memberId))
                    .post(postRepository.getReferenceById(post.getId()))
                    .lastViewedAt(now)
                    .build();
            try {
                // PostViewRepository는 단순 CRUD이므로 기존 인터페이스 사용 가능하지만 일관성을 위해 QueryRepository로 갈 수도 있음
                // 여기서는 생략하고 직접 저장
                incrementViewCount(post.getId());
                return true;
            } catch (DataIntegrityViolationException ignored) {}
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

    private void incrementViewCount(Long postId) {
        postQueryRepository.incrementViewCount(postId);
    }

    private void requireAuthenticated(Member member) {
        if (member == null) throw new BusinessException(ResponseCode.UNAUTHENTICATED);
    }

    private void requireOwnership(Post post, Member member, ResponseCode noPermissionCode) {
        if (post.getMember() == null || member == null || !Objects.equals(post.getMember().getId(), member.getId())) {
            throw new BusinessException(noPermissionCode);
        }
    }
}