package co.kr.mini_spring.post.service;

import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.CursorResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.Member;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;

        /**
         * 특정 게시글의 최상위 댓글 목록을 커서 기반 페이징으로 조회합니다.
         *
         * @param postId 게시글 ID
         * @param lastId 마지막으로 조회된 댓글 ID (첫 페이지 요청 시 null)
         * @param size   조회할 댓글 개수
         * @param currentUser 현재 로그인한 사용자 (작성자 여부 확인용)
         * @return 커서 정보와 댓글 목록을 포함한 응답 DTO
         */
        @Transactional(readOnly = true)
        public CursorResponse<CommentResponse> getComments(Long postId, Long lastId, int size, Member currentUser) {
            postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));
    
            List<Comment> comments = commentQueryRepository.findAllTopLevelCommentsByPostIdCursor(postId, lastId, size);
            
            boolean hasNext = comments.size() > size;
            List<Comment> content = hasNext ? comments.subList(0, size) : comments;
            
            Long nextCursor = content.isEmpty() ? null : content.get(content.size() - 1).getId();
            
            List<CommentResponse> responseContent = content.stream()
                    .map(comment -> new CommentResponse(comment, currentUser))
                    .collect(Collectors.toList());
                    
            return new CursorResponse<>(responseContent, nextCursor, hasNext);
        }
    /**
     * 댓글 또는 대댓글을 생성합니다.
     *
     * @param request 게시글 ID, 내용, 부모 댓글 ID(대댓글인 경우)를 포함한 요청 DTO
     * @param member  인증된 작성자 정보
     * @return 생성된 댓글 상세 정보
     */
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

    /**
     * 댓글 내용을 수정합니다. 작성자 본인만 수정할 수 있습니다.
     *
     * @param commentId 수정할 댓글 ID
     * @param request   수정할 내용이 포함된 DTO
     * @param member    인증된 수정 시도자 정보
     * @return 수정된 댓글 상세 정보
     */
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

    /**
     * 댓글을 삭제합니다.
     * - 대댓글이 달려있는 댓글인 경우, 데이터 정합성을 위해 내용을 비우고 상태만 '삭제됨'으로 변경합니다 (소프트 삭제).
     * - 대댓글이 없는 경우 DB에서 즉시 삭제합니다.
     * - 부모 댓글이 이미 '삭제됨' 상태인 경우, 마지막 자식 댓글이 삭제될 때 부모 댓글도 함께 완전히 삭제됩니다.
     *
     * @param commentId 삭제할 댓글 ID
     * @param member    인증된 삭제 시도자 정보
     */
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


}
