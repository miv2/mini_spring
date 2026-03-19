# mini_spring

리액트(React) 웹 프론트엔드 및 모바일(App) 클라이언트와 연동되는 Spring Boot 기반의 종합 API 서버입니다. 사용자 인증(웹/앱 분기), 실시간 채팅, 게시판 시스템(게시글, 댓글, 좋아요)을 위한 완벽한 RESTful API를 제공합니다.

## 핵심 기술 스택
- **Backend:** Java 17, Spring Boot 3.3.4, Gradle
- **Database:** PostgreSQL (메인 영속성), Redis (리프레시 토큰 관리, 조회수 캐싱)
- **ORM:** Spring Data JPA, Querydsl (2-Step 오프셋 페이징 최적화)
- **Security:** Spring Security, JWT (HttpOnly Cookie & Header Bearer Hybrid), OAuth2 Client (Google, Kakao)
- **Real-time:** WebSocket, STOMP (채팅 전용)
- **Infra:** Docker, Nginx, GitHub Actions (무중단 롤링 배포 구조 지원)

## 패키지 구조 (도메인 주도)
`src/main/java/co/kr/mini_spring`
- `auth`: JWT 토큰 관리 (Redis 기반), 모바일/웹 딥링크 리다이렉트 분기 핸들러 등 인증 전반.
- `member`: 회원 프로필 관리, 관련 DTO.
- `chat`: 실시간 1:1 및 그룹 채팅방, 커서 기반 메시지 조회, 참여자 및 밴 관리.
- `post`: 게시판 비즈니스 로직, 오프셋 기반 페이징 적용 (게시글, 댓글, 좋아요, 해시태그).
- `global`: 공통 예외 처리(`@RestControllerAdvice`), 공통 응답 규격(`ApiResult`), 보안 필터, Swagger, 파일 업로드 관리.

## 🔐 인증/인가 정책 (하이브리드 지원)

본 프로젝트는 단일 백엔드로 웹 브라우저와 모바일 앱을 동시에 지원하기 위한 1유저 1기기 **하이브리드 인증 구조**를 가지고 있습니다.

### 리프레시 토큰 관리 (Redis)
- DB 테이블 없이 **Redis 해시(`@RedisHash`)**로만 관리되며 TTL(7일)이 설정되어 자동 만료됩니다.
- 로그인/재로그인 시 기기 상관없이 기존 회원의 토큰은 무효화(Overwrite) 됩니다.

### 클라이언트별 소셜 로그인 분기 (`OAuth2AuthenticationSuccessHandler`)
- **웹 프론트엔드 (React):** `HttpOnly`, `Secure` 쿠키로 Access/Refresh 토큰을 자동 주입하고 `/home`으로 리다이렉트. (프론트는 `withCredentials: true`만 사용)
- **모바일 클라이언트 (App):** 소셜 로그인 요청 시 `?redirect_uri=myapp://...` 딥링크 파라미터를 넘기면, 백엔드가 쿠키 대신 해당 딥링크 URL 뒤에 쿼리스트링으로 토큰을 달아 리다이렉트하여 앱이 토큰을 안전하게 탈취할 수 있도록 지원.

## 💬 실시간 채팅 시스템
- WebSocket과 STOMP 프로토콜을 사용하며, 무한 스크롤(커서 기반 페이징)을 통해 끊김 없는 메시징 경험을 제공합니다.
- `GlobalConfig`에서 웹소켓 Handshake 및 Pub/Sub 채널 보안(Auth Channel Interceptor)이 적용되어 있습니다.

## 📝 게시글/댓글/파일
- **조회수:** Redis 캐싱을 통해 무분별한 조회수 증가 어뷰징을 방지하고 DB 부하를 줄입니다.
- **파일 첨부:** 디렉토리 공격 방지, 확장자 및 Magic Number 바이너리 검증을 거친 후 저장됩니다. DB 커밋 동기화(`TransactionSynchronizationManager`)를 통해 물리 파일과 레코드 간의 고아 데이터 발생을 완벽히 방지합니다.
- **공통 응답:** 모든 API는 `ApiResult<T>`, `PageResponse<T>`, `CursorResponse<T>` 규격으로 통일된 JSON을 반환합니다.

## 📚 API 문서
- **Swagger UI:** `http://localhost:8081/swagger-ui.html` 또는 `https://minispring.duckdns.org/swagger-ui.html`
- 전역 보안 스키마(`BearerAuth`)가 적용되어 있으며, 모든 컨트롤러 상단에 `@ApiResponse`, DTO 필드에 `@Schema` 명세가 꼼꼼히 작성되어 있습니다.

## 🚀 실행
```bash
./gradlew clean build
./gradlew bootRun
```

## ⚙️ 설정 주의사항 및 운영 규칙
- `src/main/resources/application.yml`에 포함된 민감 정보(DB, OAuth, JWT Secret, Redis 비번 등)는 운영 환경에서 모두 환경 변수(Environment Variables)로 덮어쓰기 됩니다.
- DDL은 `spring.jpa.hibernate.ddl-auto: none`으로 관리됩니다. 반드시 `scripts/db/db.sql` 파일을 통해 수동으로 DB 구조 정합성을 유지해야 합니다.
- 스키마 변경 시 `db.sql` 원본을 수정하고, 대규모 운영 변경 시에만 `202603XX_*.sql` 형식의 히스토리 스크립트를 기록합니다.