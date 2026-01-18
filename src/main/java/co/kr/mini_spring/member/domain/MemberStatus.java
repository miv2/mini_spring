package co.kr.mini_spring.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN
}
