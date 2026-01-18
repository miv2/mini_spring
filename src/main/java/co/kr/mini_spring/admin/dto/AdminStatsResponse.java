package co.kr.mini_spring.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStatsResponse {
    private final long totalMembers;
    private final long activeMembers;
    private final long suspendedMembers;
    private final long totalPosts;
    private final long publishedPosts;
    private final long totalComments;
    private final long totalLikes;
}

