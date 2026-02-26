package co.kr.mini_spring.post.service;

import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberQueryRepository;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
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

    @Value("${file.public-base-url}")
    private String publicBaseUrl;

    @Value("${file.default-profile-image:/uploads/default-profile.png}")
    private String defaultProfileImage;

    private static final Duration VIEW_COUNT_INTERVAL = Duration.ofHours(1);

    /**
     * 게시글 작성 (초기 조회수 0)
     * - @CacheEvict: 새로운 글이 작성되면 기존 목록 캐시들을 모두 삭제하여 데이터 일관성을 유지합니다.
     */
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
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
        return new PostResponse(post, author, author, 0, false, publicBaseUrl, defaultProfileImage);
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
        return new PostResponse(post, author, currentUser, viewCountOverride, isLiked, publicBaseUrl, defaultProfileImage);
    }

    /**
     * 게시글 수정 (작성자 본인만 가능)
     * - @CacheEvict: 글 내용이 수정되면 목록 캐시를 초기화합니다.
     */
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public PostResponse updatePost(Long postId, PostUpdateRequest request, SocialMember member) {
        requireAuthenticated(member);
        Post post = postQueryRepository.findByIdWithAllRelations(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        requireOwnership(post, member, ResponseCode.NO_PERMISSION_TO_UPDATE_POST);
        post.update(request.getTitle(), request.getContent());
        hashtagService.updateHashtagsForPost(post, request.getHashtags());
        return new PostResponse(post, post.getAuthor(), member, null, false, publicBaseUrl, defaultProfileImage);
    }

    /**
     * 게시글 삭제 (논리 삭제)
     * - @CacheEvict: 글이 삭제되면 목록 캐시를 초기화합니다.
     */
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public void deletePost(Long postId, SocialMember member) {
        requireAuthenticated(member);
        Post post = postQueryRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        requireOwnership(post, member, ResponseCode.NO_PERMISSION_TO_DELETE_POST);
        post.delete();
    }

    /**
     * 게시글 좋아요 추가 (비관적 락 사용)
     */
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public void addLike(Long postId, Long memberId) {
        if (memberId == null)
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        if (postLikeRepository.findById(new PostLike.PostLikeId(memberId, postId)).isPresent())
            return;

        Post post = postQueryRepository.findByIdWithPessimisticLock(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
        if (postLikeRepository.findById(new PostLike.PostLikeId(memberId, postId)).isPresent())
            return;
        PostLike newLike = PostLike.builder()
                .id(new PostLike.PostLikeId(memberId, postId))
                .post(post)
                .build();
        postLikeRepository.save(newLike);
        post.increaseLikeCount();
    }

    /**
     * 게시글 좋아요 취소
     */
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
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
    }

    /**
     * 게시글 목록 조회 (오프셋 기반 페이징)
     * - @Cacheable: 동일한 조건의 요청이 오면 DB를 조회하지 않고 Redis 캐시에서 즉시 반환합니다.
     * - value: 캐시의 네임스페이스 (posts)
     * - key: 페이지 번호, 검색어, 태그 등 파라미터를 조합하여 유일한 키를 생성합니다.
     */
    @Cacheable(value = "posts",
               key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort + ':' + #keyword + ':' + #hashtags + ':' + #authorId",
               unless = "#result == null")
    public PageResponse<PostSummaryResponse> getPublishedPosts(
            org.springframework.data.domain.Pageable pageable, String keyword, List<String> hashtags, Long authorId) {
        
        log.info("[Cache Miss] 게시글 목록을 DB에서 조회합니다. page={}, keyword={}", pageable.getPageNumber(), keyword);

        org.springframework.data.domain.Page<Post> postPage = postQueryRepository.findAllByPublishedPage(
                true, keyword, hashtags, authorId, pageable);

        List<PostSummaryResponse> responseContent = postPage.getContent().stream()
                .map(post -> new PostSummaryResponse(post, post.getAuthor(), publicBaseUrl, defaultProfileImage))
                .collect(Collectors.toList());

        return new PageResponse<>(
                responseContent,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.hasNext()
        );
    }

    /**
     * Redis 기반 조회수 업데이트 (1시간 쿨타임)
     */
    private boolean updateViewCount(Post post, SocialMember currentUser) {
        Long memberId = currentUser.getId();
        if (memberId == null || Objects.equals(post.getAuthorId(), memberId))
            return false;

        String viewKey = "post:view:member:" + memberId + ":post:" + post.getId();
        Boolean firstView = redisTemplate.opsForValue().setIfAbsent(viewKey, "viewed", VIEW_COUNT_INTERVAL);
        if (Boolean.TRUE.equals(firstView)) {
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
