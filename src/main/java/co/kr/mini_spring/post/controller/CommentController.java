package co.kr.mini_spring.post.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

import co.kr.mini_spring.global.common.response.ApiResult;
import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.post.dto.request.CommentCreateRequest;
import co.kr.mini_spring.post.dto.request.CommentUpdateRequest;
import co.kr.mini_spring.post.dto.response.CommentResponse;
import co.kr.mini_spring.post.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "댓글", description = "댓글/대댓글 작성·수정·삭제")
public class CommentController {

    private final CommentService commentService;

    /**
     * 특정 게시글의 댓글을 오프셋 기반 페이징으로 조회합니다.
     * 
     * @param postId        게시글 ID
     * @param page          페이지 번호 (0부터 시작)
     * @param size          페이지 크기
     * @param memberAdapter 인증된 사용자 정보
     * @return 댓글/대댓글 페이지 응답
     */
    @Operation(summary = "댓글 목록 조회", description = "특정 게시글의 최상위 댓글을 오프셋 기반 페이징으로 조회합니다. 대댓글은 children에 포함됩니다.")
    @GetMapping("/posts/{postId}")
    public ApiResult<PageResponse<CommentResponse>> getComments(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResult.success(commentService.getComments(postId, pageable, memberAdapter != null ? memberAdapter.getMember() : null));
    }

    /**
     * 댓글 생성
     * 
     * @param request       댓글 생성 요청 DTO (postId, content, parentId 포함)
     * @param memberAdapter 인증된 사용자 정보
     * @return 생성된 댓글 상세 정보
     */
    @PostMapping
    @Operation(summary = "댓글/대댓글 생성", description = "게시글에 댓글을 생성하거나 parentId로 대댓글을 생성합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "404", description = "게시글 또는 부모 댓글을 찾을 수 없음")
    public ApiResult<CommentResponse> createComment(
            @Valid @RequestBody CommentCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        CommentResponse response = commentService.createComment(request, memberAdapter.getMember());
        return ApiResult.success(response);
    }

    /**
     * 댓글 수정
     * 
     * @param commentId     수정할 댓글 ID
     * @param request       댓글 수정 요청 DTO (content 포함)
     * @param memberAdapter 인증된 사용자 정보
     * @return 수정된 댓글 상세 정보
     */
    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "작성자가 댓글을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (작성자 아님)")
    @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    public ApiResult<CommentResponse> updateComment(
            @Parameter(description = "수정할 댓글 ID") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        CommentResponse response = commentService.updateComment(commentId, request, memberAdapter.getMember());
        return ApiResult.success(response);
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     * 
     * @param commentId     삭제할 댓글 ID
     * @param memberAdapter 인증된 사용자 정보
     * @return 성공 응답
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "작성자가 댓글을 삭제합니다. 자식이 있으면 소프트 삭제.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (작성자 아님)")
    @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    public ApiResult<Void> deleteComment(
            @Parameter(description = "삭제할 댓글 ID") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        commentService.deleteComment(commentId, memberAdapter.getMember());
        return ApiResult.success();
    }
}
