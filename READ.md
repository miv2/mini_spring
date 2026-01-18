# mini_spring

Spring Boot API 서버입니다. (JWT 인증 + OAuth2 소셜 로그인 지원)

## 기술 스택
- Java 17 / Spring Boot 3.3.4 / Gradle
- Spring Security + JWT(Access/Refresh)
- OAuth2 Client (Google, Kakao)
- Spring Data JPA + Querydsl
- MariaDB (`src/main/resources/schema.sql`)

## 패키지 구조
`src/main/java/co/kr/mini_spring`
- `auth`: 로그인/토큰/소셜 로그인(OAuth2)
- `member`: 회원(엔티티/리포지토리/서비스/DTO)
- `post`: 게시글/댓글/좋아요/해시태그
- `global`: 공통 설정(config), 보안(security), 예외/응답(common), 유틸(util)

## 인증/인가 흐름
- 회원가입: `POST /api/v1/auth/signup` → Access/Refresh 발급 + `refresh_token` 저장
- 로그인: `POST /api/v1/auth/login` → Access/Refresh 발급 + RefreshToken 갱신
- 토큰 재발급: `POST /api/v1/auth/refresh`
- 로그아웃(인증 필요): `POST /api/v1/auth/logout` → RefreshToken 폐기(revoke)

보안 예외 경로는 `src/main/java/co/kr/mini_spring/global/config/SecurityConfig.java`에서 관리합니다.

## 게시글/댓글
- 목록/상세/작성/수정/삭제/좋아요: `src/main/java/co/kr/mini_spring/post/*`
- 조회수: `post_view`를 기준으로 “동일 회원 1시간 내 중복 증가”를 방지합니다.
- 댓글: 대댓글은 `depth=1`로 제한하며, 자식 댓글이 있는 경우 소프트 삭제를 사용합니다.
- `like_count/comment_count/view_count`는 캐시 컬럼이므로 갱신 로직이 중요합니다.

## API 문서
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- 인증이 필요한 API는 Swagger의 Authorize에 `Bearer {accessToken}` 형태로 입력합니다.
- 상세 엔드포인트 목록은 `API_DOCS.md`를 참고하세요.

## 실행
```bash
./gradlew clean build
./gradlew bootRun
```

## 설정 주의사항
- `src/main/resources/application.yml`에는 민감정보(DB/OAuth/JWT)가 포함될 수 있으니 운영에서는 반드시 환경 변수로 분리하고 값을 교체하세요.
- `spring.jpa.hibernate.ddl-auto: none` 설정이므로 DB는 `schema.sql`과 정합성을 유지해야 합니다.
