package co.kr.mini_spring.post.controller;

import co.kr.mini_spring.global.common.response.ApiResult;
import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.post.dto.request.PostCreateRequest;
import co.kr.mini_spring.post.dto.request.PostUpdateRequest;
import co.kr.mini_spring.post.dto.response.PostResponse;
import co.kr.mini_spring.post.dto.response.PostSummaryResponse;
import co.kr.mini_spring.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "게시글", description = "게시글 조회/작성/수정/삭제/좋아요")
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록 조회", description = "공개 게시글을 오프셋 기반 페이징으로 조회합니다.")
    @GetMapping
    public ApiResult<PageResponse<PostSummaryResponse>> getPublishedPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 (latest, likes, oldest)") @RequestParam(value = "sort", defaultValue = "latest") String sort,
            @Parameter(description = "제목/본문 키워드 검색") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "해시태그 이름(복수 전달 가능)") @RequestParam(value = "hashtags", required = false) List<String> hashtags,
            @Parameter(description = "작성자 ID 필터") @RequestParam(value = "authorId", required = false) Long authorId) {
        Pageable pageable = createPageable(page, size, sort);
        return ApiResult.success(postService.getPublishedPosts(pageable, keyword, hashtags, authorId));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보와 댓글/해시태그를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    public ApiResult<PostResponse> getPost(
            @Parameter(description = "조회할 게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        PostResponse response = postService.getPost(postId, memberAdapter != null ? memberAdapter.getMember() : null);
        return ApiResult.success(response);
    }

    @PostMapping
    @Operation(summary = "게시글 생성", description = "인증된 사용자가 새 게시글을 생성합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    public ApiResult<PostResponse> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResult.success(postService.createPost(request, memberAdapter.getMember()));
    }

    @PostMapping("/{postId}/likes")
    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 추가합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    public ApiResult<Void> addLike(
            @Parameter(description = "좋아요를 누를 게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        postService.addLike(postId, memberAdapter.getMember().getId());
        return ApiResult.success();
    }

    @DeleteMapping("/{postId}/likes")
    @Operation(summary = "게시글 좋아요 취소", description = "게시글의 좋아요를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    public ApiResult<Void> removeLike(
            @Parameter(description = "좋아요를 취소할 게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        postService.removeLike(postId, memberAdapter.getMember().getId());
        return ApiResult.success();
    }

    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정", description = "작성자가 게시글을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (작성자 아님)")
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    public ApiResult<PostResponse> updatePost(
            @Parameter(description = "수정할 게시글 ID") @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResult.success(postService.updatePost(postId, request, memberAdapter.getMember()));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "작성자가 게시글을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @ApiResponse(responseCode = "403", description = "권한 없음 (작성자 아님)")
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    public ApiResult<Void> deletePost(
            @Parameter(description = "삭제할 게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        postService.deletePost(postId, memberAdapter.getMember());
        return ApiResult.success();
    }

    private Pageable createPageable(int page, int size, String sort) {
        Sort sortOrder = switch (sort.toLowerCase()) {
            case "likes" -> Sort.by(Sort.Direction.DESC, "likeCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(page, size, sortOrder);
    }
}
