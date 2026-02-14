package co.kr.mini_spring.post.service;

import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberQueryRepository;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.post.cache.PostCacheKey;
import co.kr.mini_spring.post.cache.PostCacheService;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostLike;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.dto.request.PostCreateRequest;
import co.kr.mini_spring.post.dto.request.PostUpdateRequest;
import co.kr.mini_spring.post.dto.response.PostResponse;
import co.kr.mini_spring.post.dto.response.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostLikeRepository postLikeRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final SocialMemberQueryRepository socialMemberQueryRepository;
    private final HashtagService hashtagService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostCacheService postCacheService;

    @Value("${file.default-profile-image:/uploads/default-profile.png}")
    private String defaultProfileImage;

    private static final Duration VIEW_COUNT_INTERVAL = Duration.ofHours(1);

    /**
     * 게시글 작성 (초기 조회수 0)
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request, SocialMember member) {
        requireAuthenticated(member);
        SocialMember author = socialMemberQueryRepository.findByEmailWithProfileImage(member.getEmail())
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .authorId(member.getId())
                .build();
        postRepository.save(post);
        hashtagService.attachHashtagsToPost(post, request.getHashtags());
        postCacheService.evictLists();
        return new PostResponse(post, author, author, 0, false, defaultProfileImage);
    }

    /**
     * 게시글 상세 조회 (조회수 중복 방지 로직 포함)
     */
    @Transactional
    public PostResponse getPost(Long postId, SocialMember currentUser) {
        Post post = postQueryRepository.findByIdWithAllRelations(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        if (post.getAuthorId() == null)
            throw new BusinessException(ResponseCode.POST_NOT_FOUND);

        Integer viewCountOverride = null;
        if (currentUser != null && updateViewCount(post, currentUser)) {
            viewCountOverride = postQueryRepository.findViewCountById(post.getId());
        }
        SocialMember author = post.getAuthor();
        boolean isLiked = currentUser != null
                && postLikeRepository.findById(new PostLike.PostLikeId(currentUser.getId(), postId)).isPresent();
        return new PostResponse(post, author, currentUser, viewCountOverride, isLiked, defaultProfileImage);
    }

    /**
     * 게시글 수정 (작성자 본인만 가능)
     */
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request, SocialMember member) {
        requireAuthenticated(member);
        Post post = postQueryRepository.findByIdWithAllRelations(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        requireOwnership(post, member, ResponseCode.NO_PERMISSION_TO_UPDATE_POST);
        post.update(request.getTitle(), request.getContent());
        hashtagService.updateHashtagsForPost(post, request.getHashtags());
        postCacheService.evictLists();
        return new PostResponse(post, post.getAuthor(), member, null, false, defaultProfileImage);
    }

    /**
     * 게시글 삭제 (논리 삭제)
     */
    @Transactional
    public void deletePost(Long postId, SocialMember member) {
        requireAuthenticated(member);
        Post post = postQueryRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        requireOwnership(post, member, ResponseCode.NO_PERMISSION_TO_DELETE_POST);
        post.delete();
        postCacheService.evictLists();
    }

    /**
     * 게시글 좋아요 추가 (비관적 락 사용)
     */
    @Transactional
    public void addLike(Long postId, Long memberId) {
        if (memberId == null)
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        if (postLikeRepository.findById(new PostLike.PostLikeId(memberId, postId)).isPresent())
            return;

        Post post = postQueryRepository.findByIdWithPessimisticLock(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        PostLike newLike = PostLike.builder()
                .id(new PostLike.PostLikeId(memberId, postId))
                .post(post)
                .build();
        postLikeRepository.save(newLike);
        post.increaseLikeCount();
        postCacheService.evictLists();
    }

    /**
     * 게시글 좋아요 취소
     */
    @Transactional
    public void removeLike(Long postId, Long memberId) {
        if (memberId == null)
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        PostLike postLike = postLikeRepository.findById(new PostLike.PostLikeId(memberId, postId)).orElse(null);
        if (postLike == null)
            return;

        Post post = postQueryRepository.findByIdWithPessimisticLock(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        postLikeRepository.delete(postLike);
        post.decreaseLikeCount();
        postCacheService.evictLists();
    }

    /**
     * 게시글 목록 조회 (오프셋 기반 페이징)
     */
    public PageResponse<PostSummaryResponse> getPublishedPosts(
            org.springframework.data.domain.Pageable pageable, String keyword, List<String> hashtags, Long authorId) {
        String listKey = PostCacheKey.list(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString(),
                keyword,
                hashtags,
                authorId
        );
        Object cached = postCacheService.get(listKey).orElse(null);
        if (cached instanceof PageResponse<?> cachedResponse) {
            @SuppressWarnings("unchecked")
            PageResponse<PostSummaryResponse> response = (PageResponse<PostSummaryResponse>) cachedResponse;
            return response;
        }

        org.springframework.data.domain.Page<Post> postPage = postQueryRepository.findAllByPublishedPage(
                true, keyword, hashtags, authorId, pageable);

        List<PostSummaryResponse> responseContent = postPage.getContent().stream()
                .map(post -> new PostSummaryResponse(post, post.getAuthor(), defaultProfileImage))
                .collect(Collectors.toList());

        PageResponse<PostSummaryResponse> response = new PageResponse<>(
                responseContent,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.hasNext()
        );
        postCacheService.putList(listKey, response);
        return response;
    }

    /**
     * Redis 기반 조회수 업데이트 (1시간 쿨타임)
     */
    private boolean updateViewCount(Post post, SocialMember currentUser) {
        Long memberId = currentUser.getId();
        if (memberId == null || Objects.equals(post.getAuthorId(), memberId))
            return false;

        String viewKey = "post:view:member:" + memberId + ":post:" + post.getId();
        if (Boolean.FALSE.equals(redisTemplate.hasKey(viewKey))) {
            redisTemplate.opsForValue().set(viewKey, "viewed", VIEW_COUNT_INTERVAL);
            postQueryRepository.incrementViewCount(post.getId());
            return true;
        }
        return false;
    }

    private void requireAuthenticated(SocialMember member) {
        if (member == null)
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
    }

    private void requireOwnership(Post post, SocialMember member, ResponseCode noPermissionCode) {
        if (post.getAuthorId() == null || member == null || !Objects.equals(post.getAuthorId(), member.getId())) {
            throw new BusinessException(noPermissionCode);
        }
    }
}
