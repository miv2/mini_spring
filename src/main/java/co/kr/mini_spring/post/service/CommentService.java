package co.kr.mini_spring.post.service;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.post.domain.Comment;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.repository.CommentRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
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
    private final PostRepository postRepository;

    /**
     * 새로운 댓글 또는 대댓글을 생성합니다.
     *
     * @param request 댓글 생성 요청 정보 (게시글 ID, 내용, 부모 댓글 ID 포함)
     * @param member  댓글을 작성하는 회원 정보
     * @return 생성된 댓글의 상세 정보 (CommentResponse)
     * @throws BusinessException 게시글을 찾을 수 없거나, 부모 댓글이 존재하지 않거나 해당 게시글에 속하지 않을 경우 예외 발생
     */
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, Member member) {
        // 1. 게시글 조회
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

        // 2. 부모 댓글 조회 및 검증 (대댓글인 경우)
        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));
            // 부모 댓글이 요청된 게시글에 속해 있는지 확인
            if (!Objects.equals(parentComment.getPost().getId(), post.getId())) {
                throw new BusinessException(ResponseCode.COMMENT_NOT_BELONG_TO_POST);
            }
        }

        // 3. 댓글 엔티티 생성
        Comment comment = Comment.builder()
                .content(request.getContent())
                .member(member)
                .post(post)
                .parent(parentComment)
                .depth(parentComment == null ? 0 : 1) // 최상위 댓글은 depth 0, 대댓글은 depth 1
                .build();

        // 4. 부모 댓글의 자식 목록에 추가 (양방향 연관관계 편의 메서드 역할)
        if (parentComment != null) {
            parentComment.getChildren().add(comment);
        }

        // 5. 댓글 저장 및 게시글의 댓글 수 증가
        Comment savedComment = commentRepository.save(comment);
        postRepository.incrementCommentCount(post.getId());

        return new CommentResponse(savedComment, member);
    }

    /**
     * 기존 댓글의 내용을 수정합니다.
     *
     * @param commentId 수정할 댓글의 ID
     * @param request   수정할 내용이 담긴 요청 정보
     * @param member    수정을 요청한 회원 정보
     * @return 수정된 댓글의 상세 정보 (CommentResponse)
     * @throws BusinessException 댓글을 찾을 수 없거나, 수정 권한이 없는 경우(작성자가 아님) 예외 발생
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Member member) {
        // 1. 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));

        // 2. 권한 검증 (작성자 본인인지 확인)
        if (comment.getMember() == null || member == null || !Objects.equals(comment.getMember().getId(), member.getId())) {
            throw new BusinessException(ResponseCode.NO_PERMISSION_TO_UPDATE_COMMENT);
        }

        // 3. 내용 수정 (Dirty Checking에 의해 트랜잭션 종료 시 자동 반영)
        comment.updateContent(request.getContent());

        return new CommentResponse(comment, member);
    }

    /**
     * 댓글을 삭제합니다.
     * 자식 댓글(대댓글)이 있는 경우 '삭제된 댓글입니다'로 내용을 변경하는 소프트 삭제를 수행하고,
     * 자식 댓글이 없는 경우 데이터베이스에서 완전히 삭제(하드 삭제)합니다.
     *
     * @param commentId 삭제할 댓글의 ID
     * @param member    삭제를 요청한 회원 정보
     * @throws BusinessException 댓글을 찾을 수 없거나, 삭제 권한이 없는 경우 예외 발생
     */
    @Transactional
    public void deleteComment(Long commentId, Member member) {
        // 1. 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COMMENT_NOT_FOUND));
        Long postId = comment.getPost().getId();

        // 2. 권한 검증
        if (comment.getMember() == null || member == null || !Objects.equals(comment.getMember().getId(), member.getId())) {
            throw new BusinessException(ResponseCode.NO_PERMISSION_TO_DELETE_COMMENT);
        }

        // 3. 삭제 로직 수행
        if (!comment.getChildren().isEmpty()) {
            // Case A: 자식 댓글이 있는 경우 -> 소프트 삭제 (isDeleted = true)
            comment.markAsDeleted();
        } else {
            // Case B: 자식 댓글이 없는 경우 -> 하드 삭제 (DB에서 제거)
            commentRepository.delete(comment);
            postRepository.decrementCommentCount(postId); // 댓글 수 감소

            // 부모 댓글과의 연관관계 끊기
            if (comment.getParent() != null) {
                comment.getParent().getChildren().remove(comment);
            }

            // 추가 로직: 부모 댓글이 이미 삭제된 상태(소프트 삭제)였고, 방금 삭제로 인해 더 이상 자식이 없다면 부모도 하드 삭제
            if (comment.getParent() != null && comment.getParent().isDeleted() && comment.getParent().getChildren().stream().allMatch(Comment::isDeleted)) {
                commentRepository.delete(comment.getParent());
                postRepository.decrementCommentCount(postId); // 부모 댓글 삭제에 따른 댓글 수 감소
            }
        }
    }

    /**
     * 특정 게시글의 최상위 댓글 목록을 페이징하여 조회합니다.
     * 대댓글은 각 댓글의 `children` 필드에 포함되어 반환됩니다.
     *
     * @param postId 조회할 게시글의 ID
     * @param page   페이지 번호 (0부터 시작)
     * @param size   페이지 당 댓글 수
     * @return 페이징된 댓글 목록 응답 (PageResponse<CommentResponse>)
     * @throws BusinessException 게시글을 찾을 수 없는 경우 예외 발생
     */
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(Long postId, int page, int size) {
        // 1. 게시글 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ResponseCode.POST_NOT_FOUND));

        // 2. 최상위 댓글(parent == null)만 페이징 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtDesc(postId, pageable);

        // 3. DTO 변환 (대댓글 구조는 CommentResponse 생성자 내부에서 재귀적으로 처리됨)
        Page<CommentResponse> dtoPage = commentPage.map(comment -> new CommentResponse(comment, null));

        return new PageResponse<>(dtoPage);
    }
}
