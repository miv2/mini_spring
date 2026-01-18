package co.kr.mini_spring.post.controller;

import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.post.dto.request.PostCreateRequest;
import co.kr.mini_spring.post.dto.request.PostUpdateRequest;
import co.kr.mini_spring.post.dto.response.PostResponse;
import co.kr.mini_spring.post.dto.response.PostSummaryResponse;
import co.kr.mini_spring.post.service.PostService;
import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "게시글", description = "게시글 조회/작성/수정/삭제/좋아요")
public class PostController {

    private final PostService postService;

    /**
     * 모든 공개된 게시글 목록을 페이징하여 조회합니다.
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 당 게시글 수
     * @param sort 정렬 기준 (recent, likes, oldest)
     * @param keyword 제목/본문 키워드 검색
     * @param hashtags 해시태그 이름(복수) 필터
     * @param authorId 작성자 ID 필터
     * @return 페이징된 게시글 목록
     */
    @Operation(summary = "게시글 목록 조회", description = "공개 게시글을 페이징/검색/필터/정렬하여 조회합니다.")
    @GetMapping
    public ApiResponse<PageResponse<PostSummaryResponse>> getPublishedPosts(
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "정렬 기준(recent|likes|oldest)") @RequestParam(value = "sort", defaultValue = "recent") String sort,
            @Parameter(description = "제목/본문 키워드 검색") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "해시태그 이름(복수 전달 가능)") @RequestParam(value = "hashtags", required = false) List<String> hashtags,
            @Parameter(description = "작성자 ID 필터") @RequestParam(value = "authorId", required = false) Long authorId
    ) {
        Pageable pageable = createPageable(page, size, sort);
        PageResponse<PostSummaryResponse> response = postService.getPublishedPosts(pageable, keyword, hashtags, authorId);
        return ApiResponse.success(response);
    }

    /**
     * 특정 게시글 상세 조회
     * @param postId 조회할 게시글 ID
     * @param memberAdapter (Optional) 인증된 사용자 정보
     * @return 게시글 상세 정보
     */
    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보와 댓글/해시태그를 반환합니다.")
    public ApiResponse<PostResponse> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal MemberAdapter memberAdapter
    ) {
        PostResponse response = postService.getPost(postId, memberAdapter != null ? memberAdapter.getMember() : null);
        return ApiResponse.success(response);
    }

    /**
     * 게시글 생성
     * @param request 게시글 생성 요청 DTO
     * @param memberAdapter 인증된 사용자 정보
     * @return 생성된 게시글 상세 정보
     */
    @PostMapping
    @Operation(summary = "게시글 생성", description = "인증된 사용자가 새 게시글을 생성합니다.")
    public ApiResponse<PostResponse> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal MemberAdapter memberAdapter
    ) {
        PostResponse response = postService.createPost(request, memberAdapter.getMember());
        return ApiResponse.success(response);
    }

    /**
     * 게시글 좋아요 추가
     * @param postId 좋아요를 누를 게시글 ID
     * @param memberAdapter 인증된 사용자 정보
     * @return 성공 응답
     */
    @PostMapping("/{postId}/likes")
    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 추가합니다.")
    public ApiResponse<Void> addLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal MemberAdapter memberAdapter
    ) {
        postService.addLike(postId, memberAdapter.getMember().getId());
        return ApiResponse.success();
    }

    /**
     * 게시글 좋아요 취소
     * @param postId 좋아요를 취소할 게시글 ID
     * @param memberAdapter 인증된 사용자 정보
     * @return 성공 응답
     */
    @DeleteMapping("/{postId}/likes")
    @Operation(summary = "게시글 좋아요 취소", description = "게시글의 좋아요를 취소합니다.")
    public ApiResponse<Void> removeLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal MemberAdapter memberAdapter
    ) {
        postService.removeLike(postId, memberAdapter.getMember().getId());
        return ApiResponse.success();
    }

    /**
     * 게시글 수정
     * @param postId 수정할 게시글 ID
     * @param request 게시글 수정 요청 DTO
     * @param memberAdapter 인증된 사용자 정보
     * @return 수정된 게시글 상세 정보
     */
    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정", description = "작성자가 게시글을 수정합니다.")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal MemberAdapter memberAdapter
    ) {
        PostResponse response = postService.updatePost(postId, request, memberAdapter.getMember());
        return ApiResponse.success(response);
    }

    /**
     * 게시글 삭제
     * @param postId 삭제할 게시글 ID
     * @param memberAdapter 인증된 사용자 정보
     * @return 성공 응답
     */
    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "작성자가 게시글을 삭제합니다.")
    public ApiResponse<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal MemberAdapter memberAdapter
    ) {
        postService.deletePost(postId, memberAdapter.getMember());
        return ApiResponse.success();
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
