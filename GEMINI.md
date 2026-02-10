# 프로젝트 컨텍스트: mini_spring

## 프로젝트 개요
`mini_spring`은 플러터(Flutter) 모바일 애플리케이션과 웹 관리자 패널(예정)의 백엔드 역할을 수행하는 Spring Boot API 서버입니다. 사용자 인증, 소셜 로그인, 회원 관리, 그리고 게시판 시스템(게시글, 댓글, 좋아요)을 위한 RESTful API를 제공합니다.

### 주요 기술 스택
- **언어:** Java 17
- **프레임워크:** Spring Boot 3.3.4
- **빌드 도구:** Gradle
- **데이터베이스:** MariaDB (메인), Redis (인프라 구축 완료, 앱 로직 연동 대기 중)
- **ORM:** Spring Data JPA + Querydsl
- **보안:** Spring Security, JWT (Access/Refresh Token), OAuth2 Client (Google, Kakao)
- **문서화:** Swagger UI (`/swagger-ui.html`)
- **배포:** Docker, GitHub Actions, Nginx (리버스 프록시)

## 아키텍처 및 패키지 구조
이 프로젝트는 `src/main/java/co/kr/mini_spring` 하위에 도메인 주도 설계(DDD)의 영향을 받은 패키지 구조를 따릅니다.

- **`auth`**: 인증 로직, JWT 토큰 관리, OAuth2 핸들러 및 서비스.
- **`member`**: 회원 엔티티, 리포지토리, 서비스, DTO.
- **`post`**: 게시글, 댓글, 좋아요, 해시태그, 조회수 등 게시판 기능.
- **`global`**:
    - **`config`**: 보안(`SecurityConfig.java`), 웹, Swagger 설정.
    - **`common`**: 전역 예외 처리(`GlobalExceptionHandler`), 공통 응답(`ApiResponse`).
    - **`security`**: JWT 필터, UserDetails 구현체.
    - **`util`**: 유틸리티 클래스.

## 설정 및 환경 관리
Spring Profiles를 사용하여 환경별 설정을 관리합니다.

### 1. `src/main/resources/application.yml` (공통)
- 모든 환경에서 공유하는 기본 설정.
- 기본 활성 프로필(`active profile`)을 `local`로 설정.

### 2. `src/main/resources/application-prod.yml` (운영 환경)
- 실제 배포 시 사용.
- **중요:** 민감한 정보(예: `${SPRING_DATASOURCE_URL}`, `${JWT_SECRET}`)는 환경 변수로 처리하여 보안 강화.
- **참고:** OAuth2 `redirect-uri`는 하드코딩된 IP 대신 `${APP_BASE_URL}` 환경 변수를 사용.

### 3. `src/main/resources/application-local.yml` (로컬 환경 - Git 제외)
- 로컬 개발용 설정.
- 실제 DB 접속 정보나 로컬용 시크릿 키 등을 포함.
- **보안:** Git 추적에서 제외(`.gitignore`)되어 정보 유출 방지.

## 빌드 및 실행

### 로컬 개발
```bash
# 프로젝트 빌드
./gradlew clean build

# 애플리케이션 실행
./gradlew bootRun
```

### Docker 실행
`eclipse-temurin:17-jdk-focal` 기반의 경량 이미지를 생성하는 `Dockerfile`이 포함되어 있습니다.
```bash
# Docker 이미지 빌드
docker build -t mini_spring .

# 컨테이너 실행 (예시)
docker run -p 8081:8081 -e SPRING_PROFILES_ACTIVE=prod mini_spring
```

### 배포 (CI/CD)
- **워크플로우:** `.github/workflows/deploy.yml`
- **프로세스:**
    1.  `main` 브랜치에 푸시(Push) 발생 시 워크플로우 트리거.
    2.  Gradle을 사용하여 JAR 빌드.
    3.  Docker 이미지를 빌드하고 Docker Hub에 푸시.
    4.  SSH를 통해 원격 서버에 접속.
    5.  새 이미지를 풀(Pull) 받고 `docker compose`를 통해 재배포 (헬스 체크 및 롤백 로직 포함).

## 개발 컨벤션

### 인증 흐름
- **소셜 로그인:** OAuth2 (Google/Kakao) 인증 -> 백엔드에서 Access/Refresh Token과 함께 리다이렉트.
- **토큰 저장:**
    - **앱 (Flutter):** 현재 JSON Body 또는 URL Fragment 방식으로 토큰을 전달하며, 기기의 보안 저장소(Secure Storage)에 보관하는 것을 권장.
    - **웹:** 추후 웹 확장 시 Refresh Token은 HttpOnly Cookie 사용을 권장(XSS 방지).
- **CORS:** `SecurityConfig.java`에 설정됨. 현재 `http://localhost:5173` 허용 중이며, 운영 배포 시 실제 도메인 추가 필요.

### 코딩 표준
- **DTO:** API 요청/응답 시 반드시 DTO를 사용 (Entity 직접 노출 금지).
- **Entity:** `@Builder` 패턴 사용 및 기본 생성자는 `protected`로 제한.
- **트랜잭션:** 조회 메서드에는 `@Transactional(readOnly = true)` 적용.
- **DataBase:** DDL 작성시 주석 작성

### 커밋 컨벤션
- **한글 및 이모지(Gitmoji) 사용:** 커밋 메시지는 이모지를 포함하여 한글로 간결하고 담백하게 작성합니다.
- **예시:** `✨ feat: 소셜 로그인 기능 추가`

## 인프라 (원격 서버)
- **OS:** Ubuntu 24.04 LTS
- **서버 IP:** `141.11.9.2`
- **구성 요소:**
    - Nginx (리버스 프록시, 80 포트 리스닝)
    - Spring Boot App (컨테이너, 내부 8081 포트)
    - MariaDB (컨테이너/호스트)
    - Redis (컨테이너, 6379 포트)
- **시크릿 관리:** 서버 내부의 `.env` 파일을 통해 환경 변수를 `docker-compose`에 주입.

## 문서 컨벤션
    -  변경사항이 있다면 GEMINI.md 최신화 