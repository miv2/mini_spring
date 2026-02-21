package co.kr.mini_spring.post.service;

import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberQueryRepository;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.post.domain.Comment;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.repository.CommentQueryRepository;
import co.kr.mini_spring.post.domain.repository.CommentRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.dto.request.CommentCreateRequest;
import co.kr.mini_spring.post.dto.request.CommentUpdateRequest;
import co.kr.mini_spring.post.dto.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final SocialMemberQueryRepository socialMemberQueryRepository;

    @Value("${file.public-base-url}")
    private String publicBaseUrl;

    @Value("${file.default-profile-image:/uploads/default-profile.png}")
    private String defaultProfileImage;

    /**
     * 특정 게시글의 최상위 댓글 목록을 오프셋 기반 페이징으로 조회합니다.
     */
    public PageResponse<CommentResponse> getComments(Long postId, Pageable pageable, SocialMember currentUser) {
        postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

        Page<Comment> commentPage = commentQueryRepository.findAllTopLevelCommentsByPostIdPage(postId, pageable);
        List<Comment> content = commentPage.getContent();

        // 모든 댓글(최상위 + 대댓글)의 작성자 ID를 한 번에 수집 (N+1 방지)
        Set<Long> authorIds = content.stream()
                .flatMap(c -> {
                    List<Long> ids = new ArrayList<>();
                    if (c.getAuthorId() != null) ids.add(c.getAuthorId());
                    c.getChildren().forEach(child -> {
                        if (child.getAuthorId() != null) ids.add(child.getAuthorId());
                    });
                    return ids.stream();
                })
                .collect(Collectors.toSet());

        Map<Long, SocialMember> authorMap;
        if (authorIds.isEmpty()) {
            authorMap = new HashMap<>();
        } else {
            authorMap = socialMemberQueryRepository.findAllByIdWithProfileImage(authorIds).stream()
                    .collect(Collectors.toMap(
                            SocialMember::getId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));
        }

        List<CommentResponse> responseContent = content.stream()
                .map(comment -> mapToCommentResponse(comment, currentUser, authorMap))
                .collect(Collectors.toList());

        return new PageResponse<>(
                responseContent,
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getTotalElements(),
                commentPage.getTotalPages(),
                commentPage.hasNext()
        );
    }

    /**
     * 댓글 또는 대댓글을 생성합니다.
     * - @CacheEvict: 댓글이 생성되면 글 목록의 댓글 수 정보 갱신을 위해 'posts' 캐시를 삭제합니다.
     */
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public CommentResponse createComment(CommentCreateRequest request, SocialMember member) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

        SocialMember author = socialMemberQueryRepository.findByEmailWithProfileImage(member.getEmail())
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));
            if (!Objects.equals(parentComment.getPost().getId(), post.getId())) {
                throw new BusinessException(ResponseCode.COMMENT_NOT_BELONG_TO_POST);
            }
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .authorId(member.getId())
                .post(post)
                .parent(parentComment)
                .depth(parentComment == null ? 0 : 1)
                .build();

        if (parentComment != null) {
            parentComment.getChildren().add(comment);
        }

        Comment savedComment = commentRepository.save(comment);
        postQueryRepository.incrementCommentCount(post.getId());

        return new CommentResponse(savedComment, author, author, publicBaseUrl, defaultProfileImage);
    }

    /**
     * 댓글 내용을 수정합니다. 작성자 본인만 수정할 수 있습니다.
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, SocialMember member) {
        Comment comment = commentQueryRepository.findByIdWithMember(commentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));

        if (comment.getAuthorId() == null || member == null || !Objects.equals(comment.getAuthorId(), member.getId())) {
            throw new BusinessException(ResponseCode.NO_PERMISSION_TO_UPDATE_COMMENT);
        }

        SocialMember author = socialMemberQueryRepository.findByEmailWithProfileImage(member.getEmail())
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        comment.updateContent(request.getContent());
        return new CommentResponse(comment, author, author, publicBaseUrl, defaultProfileImage);
    }

    /**
     * 댓글을 삭제합니다.
     * - @CacheEvict: 댓글이 삭제되면 글 목록의 댓글 수 정보 갱신을 위해 'posts' 캐시를 삭제합니다.
     */
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public void deleteComment(Long commentId, SocialMember member) {
        Comment comment = commentQueryRepository.findByIdWithMember(commentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));
        Long postId = comment.getPost().getId();

        if (comment.getAuthorId() == null || member == null || !Objects.equals(comment.getAuthorId(), member.getId())) {
            throw new BusinessException(ResponseCode.NO_PERMISSION_TO_DELETE_COMMENT);
        }

        if (!comment.getChildren().isEmpty()) {
            comment.delete();
        } else {
            commentRepository.delete(comment);
            postQueryRepository.decrementCommentCount(postId);
            if (comment.getParent() != null) {
                comment.getParent().getChildren().remove(comment);
            }
            if (comment.getParent() != null && comment.getParent().isDeleted()
                    && comment.getParent().getChildren().isEmpty()) {
                commentRepository.delete(comment.getParent());
                postQueryRepository.decrementCommentCount(postId);
            }
        }
    }

    private CommentResponse mapToCommentResponse(Comment comment, SocialMember currentUser,
            Map<Long, SocialMember> authorMap) {
        SocialMember author = authorMap.get(comment.getAuthorId());
        List<CommentResponse> children = comment.getChildren().stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .map(child -> mapToCommentResponse(child, currentUser, authorMap))
                .collect(Collectors.toList());
        return new CommentResponse(comment, author, currentUser, children, publicBaseUrl, defaultProfileImage);
    }
}