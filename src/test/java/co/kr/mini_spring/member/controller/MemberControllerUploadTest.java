package co.kr.mini_spring.member.controller;

import co.kr.mini_spring.global.config.SecurityConfig;
import co.kr.mini_spring.global.security.CustomUserDetailsService;
import co.kr.mini_spring.global.security.JwtAccessDeniedHandler;
import co.kr.mini_spring.global.security.JwtAuthenticationEntryPoint;
import co.kr.mini_spring.global.security.JwtTokenProvider;
import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.auth.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import co.kr.mini_spring.auth.oauth.handler.OAuth2AuthenticationFailureHandler;
import co.kr.mini_spring.auth.oauth.handler.OAuth2AuthenticationSuccessHandler;
import co.kr.mini_spring.auth.oauth.service.CustomOAuth2UserService;
import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@AutoConfigureMockMvc
class MemberControllerUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @MockBean
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Test
    @DisplayName("프로필 이미지 업로드 성공")
    void updateProfileImage_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cat.png",
                "image/png",
                pngBytes()
        );

        when(memberService.updateProfileImage(eq(1L), any())).thenReturn("/uploads/2026/02/04/cat.png");

        Member member = Member.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .name("테스터")
                .nickname("테스터")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .oauthProvider(MemberProvider.LOCAL)
                .oauthId(null)
                .build();
        ReflectionTestUtils.setField(member, "id", 1L);

        MemberAdapter adapter = new MemberAdapter(member);
        Authentication auth = new UsernamePasswordAuthenticationToken(adapter, null, adapter.getAuthorities());

        mockMvc.perform(multipart("/api/v1/members/me/profile-image")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data").value("/uploads/2026/02/04/cat.png"));
    }

    @Test
    @DisplayName("인증 없이 프로필 이미지 업로드 시 401")
    void updateProfileImage_unauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cat.png",
                "image/png",
                pngBytes()
        );

        mockMvc.perform(multipart("/api/v1/members/me/profile-image")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
