package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.post.domain.Comment;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.repository.CommentRepository;
import co.kr.mini_spring.post.domain.repository.CommentQueryRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.domain.repository.PostQueryRepository;
import co.kr.mini_spring.post.dto.request.CommentCreateRequest;
import co.kr.mini_spring.post.dto.request.CommentUpdateRequest;
import co.kr.mini_spring.post.dto.response.CommentResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;

    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, Member member) {
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
                .member(member)
                .post(post)
                .parent(parentComment)
                .depth(parentComment == null ? 0 : 1)
                .build();

        if (parentComment != null) {
            parentComment.getChildren().add(comment);
        }

        Comment savedComment = commentRepository.save(comment);
        postQueryRepository.incrementCommentCount(post.getId());

        return new CommentResponse(savedComment, member);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Member member) {
        Comment comment = commentQueryRepository.findByIdWithMember(commentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));

        if (comment.getMember() == null || member == null || !Objects.equals(comment.getMember().getId(), member.getId())) {
            throw new BusinessException(ResponseCode.NO_PERMISSION_TO_UPDATE_COMMENT);
        }

        comment.updateContent(request.getContent());
        return new CommentResponse(comment, member);
    }

    @Transactional
    public void deleteComment(Long commentId, Member member) {
        Comment comment = commentQueryRepository.findByIdWithMember(commentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));
        Long postId = comment.getPost().getId();

        if (comment.getMember() == null || member == null || !Objects.equals(comment.getMember().getId(), member.getId())) {
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
            if (comment.getParent() != null && comment.getParent().isDeleted() && comment.getParent().getChildren().isEmpty()) {
                commentRepository.delete(comment.getParent());
                postQueryRepository.decrementCommentCount(postId);
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(Long postId, int page, int size) {
        postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentQueryRepository.findAllTopLevelCommentsByPostId(postId, pageable);

        return new PageResponse<>(commentPage.map(comment -> new CommentResponse(comment, null)));
    }
}
