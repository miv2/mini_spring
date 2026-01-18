package co.kr.mini_spring.member.service;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.member.dto.request.SignUpRequest;
import co.kr.mini_spring.member.dto.response.SignUpResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback; // Rollback import 추가
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공 - DB에 데이터가 정상적으로 저장되어야 한다")
    public void signUp_success() {
        // given
        String email = "test@example.com";
        String password = "password123!";
        String name = "테스터";
        String nickname = "테스터닉네임";

        SignUpRequest request = createSignUpRequest(email, password, name, nickname);

        // when
        SignUpResponse response = memberService.signUp(request);

        // then
        // 1. 응답값 검증
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getName()).isEqualTo(name);

        // 2. DB 저장 검증
        Member savedMember = memberRepository.findByEmail(email).orElseThrow();
        assertThat(savedMember.getEmail()).isEqualTo(email);
        assertThat(savedMember.getName()).isEqualTo(name);
        assertThat(savedMember.getNickname()).isEqualTo(nickname);
        
        // 3. 비밀번호 암호화 검증
        assertThat(savedMember.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, savedMember.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일로 가입하면 예외가 발생해야 한다")
    public void signUp_fail_duplicate_email() {
        // given
        String email = "duplicate@example.com";
        
        memberRepository.save(Member.builder()
                .email(email)
                .passwordHash("anyPass")
                .name("기존회원")
                .nickname("기존닉네임")
                .build());

        SignUpRequest request = createSignUpRequest(email, "newPass", "신규회원", "신규닉네임");

        // when & then
        assertThatThrownBy(() -> memberService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.DUPLICATE_MEMBER_EMAIL);
    }

    private SignUpRequest createSignUpRequest(String email, String password, String name, String nickname) {
        SignUpRequest request = new SignUpRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setPasswordConfirm(password);
        request.setName(name);
        request.setNickname(nickname);
        return request;
    }
}
