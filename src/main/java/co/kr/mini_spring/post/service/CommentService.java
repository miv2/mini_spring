package co.kr.mini_spring.post.service;

import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.post.cache.PostCacheKey;
import co.kr.mini_spring.post.cache.PostCacheService;
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
    private final PostCacheService postCacheService;

    /**
     * 특정 게시글의 최상위 댓글 목록을 오프셋 기반 페이징으로 조회합니다.
     *
     * @param postId      게시글 ID
     * @param pageable    페이지 정보
     * @param currentUser 현재 로그인한 사용자 (작성자 여부 확인용)
     * @return 댓글 목록을 포함한 응답 DTO
     */
    public PageResponse<CommentResponse> getComments(Long postId, Pageable pageable, SocialMember currentUser) {
        postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

        Page<Comment> commentPage = commentQueryRepository.findAllTopLevelCommentsByPostIdPage(postId, pageable);
        List<Comment> content = commentPage.getContent();

        Set<Long> authorIds = content.stream()
                .map(Comment::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        content.forEach(c -> {
            c.getChildren().forEach(child -> {
                if (child.getAuthorId() != null)
                    authorIds.add(child.getAuthorId());
            });
        });

        Map<Long, SocialMember> authorMap = socialMemberRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(SocialMember::getId, Function.identity()));

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
     *
     * @param request 게시글 ID, 내용, 부모 댓글 ID(대댓글인 경우)를 포함한 요청 DTO
     * @param member  인증된 작성자 정보
     * @return 생성된 댓글 상세 정보
     */
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, SocialMember member) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

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
        postCacheService.evict(PostCacheKey.detail(post.getId()));
        postCacheService.evictLists();

        return new CommentResponse(savedComment, member, member, List.of());
    }

    /**
     * 댓글 내용을 수정합니다. 작성자 본인만 수정할 수 있습니다.
     *
     * @param commentId 수정할 댓글 ID
     * @param request   수정할 내용이 포함된 DTO
     * @param member    인증된 수정 시도자 정보
     * @return 수정된 댓글 상세 정보
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, SocialMember member) {
        Comment comment = commentQueryRepository.findByIdWithMember(commentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));

        if (comment.getAuthorId() == null || member == null || !Objects.equals(comment.getAuthorId(), member.getId())) {
            throw new BusinessException(ResponseCode.NO_PERMISSION_TO_UPDATE_COMMENT);
        }

        comment.updateContent(request.getContent());
        return new CommentResponse(comment, member, member, List.of());
    }

    /**
     * 댓글을 삭제합니다.
     *
     * @param commentId 삭제할 댓글 ID
     * @param member    인증된 삭제 시도자 정보
     */
    @Transactional
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
        postCacheService.evict(PostCacheKey.detail(postId));
        postCacheService.evictLists();
    }

    private CommentResponse mapToCommentResponse(Comment comment, SocialMember currentUser,
            Map<Long, SocialMember> authorMap) {
        SocialMember author = authorMap.get(comment.getAuthorId());
        List<CommentResponse> children = comment.getChildren().stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .map(child -> mapToCommentResponse(child, currentUser, authorMap))
                .collect(Collectors.toList());
        return new CommentResponse(comment, author, currentUser, children);
    }
}
