package co.kr.mini_spring.admin.controller;

import co.kr.mini_spring.admin.dto.AdminStatsResponse;
import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.post.domain.repository.PostLikeRepository;
import co.kr.mini_spring.post.domain.repository.PostRepository;
import co.kr.mini_spring.post.domain.repository.CommentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "관리자", description = "관리자 대시보드 API")
public class AdminController {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    @Operation(summary = "대시보드 요약 통계", description = "회원/게시글/댓글/좋아요 집계 통계를 반환합니다. (ADMIN 전용)")
    @GetMapping("/stats")
    public ApiResponse<AdminStatsResponse> getStats() {
        long totalMembers = memberRepository.count();
        long activeMembers = memberRepository.countByStatus(MemberStatus.ACTIVE);
        long suspendedMembers = memberRepository.countByStatus(MemberStatus.SUSPENDED);
        long totalPosts = postRepository.count();
        long publishedPosts = postRepository.countByPublished(true);
        long totalComments = commentRepository.count();
        long totalLikes = postLikeRepository.count();

        AdminStatsResponse response = AdminStatsResponse.builder()
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .suspendedMembers(suspendedMembers)
                .totalPosts(totalPosts)
                .publishedPosts(publishedPosts)
                .totalComments(totalComments)
                .totalLikes(totalLikes)
                .build();

        return ApiResponse.success(response);
    }
}
