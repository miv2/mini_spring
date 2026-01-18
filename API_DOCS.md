# mini_spring API 명세서

이 문서는 `mini_spring` 프로젝트의 API 엔드포인트, 데이터 구조 및 에러 코드에 대한 정보를 제공합니다.

---

## 1. 개요 및 Swagger 문서

본 프로젝트는 RESTful API 원칙을 따르며, 모든 응답은 JSON 형식으로 제공됩니다.

- **Swagger UI**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **OpenAPI Spec**: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)
- **Base URL**: `http://localhost:8081`

인증이 필요한 API는 헤더에 `Authorization: Bearer {accessToken}`을 포함해야 합니다.

---

## 2. 공통 응답 형식 (`ApiResponse`)

모든 API 응답은 아래와 같은 공통 래퍼(Wrapper)에 담겨 반환됩니다.

```json
{
  "success": true,
  "code": "S001",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... },
  "timestamp": "2026-01-18 12:00:00"
}
```

- `success`: 요청 처리 성공 여부 (true/false)
- `code`: 비즈니스 응답 코드 (성공: Sxxx, 에러: Cxxx, Pxxx 등)
- `message`: 응답 메시지
- `data`: 실제 데이터 객체 (성공 시에만 포함, 데이터가 없으면 null)
- `timestamp`: 응답 생성 시각

---

## 3. 에러 코드 정의 (`ResponseCode`)

| 코드 | HTTP 상태 | 메시지 | 설명 |
| :--- | :--- | :--- | :--- |
| **C001** | 400 | 잘못된 입력 값입니다. | 유효성 검사 실패 등 |
| **C003** | 403 | 접근이 거부되었습니다. | 권한 부족 |
| **C004** | 401 | 인증되지 않은 사용자입니다. | 토큰 누락 또는 비로그인 |
| **A001** | 401 | 유효하지 않은 토큰입니다. | JWT 변조 또는 잘못된 형식 |
| **A002** | 401 | 만료된 토큰입니다. | AccessToken 만료 (Refresh 필요) |
| **P001** | 404 | 게시글을 찾을 수 없습니다. | 존재하지 않는 postId |
| **M003** | 409 | 중복된 회원 이메일입니다. | 회원가입 시 중복 이메일 사용 |

*(상세한 코드는 `src/main/java/co/kr/mini_spring/global/common/response/ResponseCode.java` 참조)*

---

## 4. 인증 API (`/api/v1/auth`)

### 4.1. 회원가입
- **Endpoint**: `POST /api/v1/auth/signup`
- **Request**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123!",
    "passwordConfirm": "password123!",
    "name": "홍길동",
    "nickname": "길동이"
  }
  ```
- **Response**: `201 Created`와 함께 회원 정보 반환.

### 4.2. 로그인
- **Endpoint**: `POST /api/v1/auth/login`
- **Request**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123!"
  }
  ```
- **Response**:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbG...",
      "refreshToken": "eyJhbG...",
      "accessTokenExpiresAt": "2026-01-18T13:00:00",
      "refreshTokenExpiresAt": "2026-01-25T12:00:00"
    }
  }
  ```

---

## 5. 소셜 로그인 API (`/api/v1/auth/social`)

### 5.1. 제공자 목록 및 URL 조회
- **Endpoint**: `GET /api/v1/auth/social/providers`
- **Description**: 지원하는 소셜 로그인(Google, Kakao) 링크를 반환합니다.
- **Response Example**:
  ```json
  {
    "success": true,
    "data": [
      {
        "provider": "google",
        "displayName": "Google",
        "authorizationUrl": "http://localhost:8081/oauth2/authorization/google"
      }
    ]
  }
  ```

---

## 6. 게시글 API (`/api/v1/posts`)

### 6.1. 게시글 목록 조회
- **Endpoint**: `GET /api/v1/posts`
- **Query Params**:
  - `page`: 페이지 번호 (기본 0)
  - `size`: 페이지 크기 (기본 10)
  - `sort`: 정렬 (`recent`, `likes`, `oldest`)
  - `keyword`: 검색 키워드
- **Response**: `PageResponse` 형식으로 반환.

### 6.2. 게시글 상세 조회
- **Endpoint**: `GET /api/v1/posts/{postId}`
- **Response Example**:
  ```json
  {
    "success": true,
    "data": {
      "id": 1,
      "title": "안녕 하세요",
      "content": "본문 내용입니다.",
      "viewCount": 10,
      "likeCount": 5,
      "memberName": "길동이",
      "isOwner": false,
      "hashtags": ["spring", "java"],
      "comments": [ ... ],
      "createdAt": "2026-01-18 10:00"
    }
  }
  ```

### 6.3. 게시글 좋아요 토글
- **Endpoint**: `POST /api/v1/posts/{postId}/likes` (추가) / `DELETE /api/v1/posts/{postId}/likes` (취소)
- **Description**: 현재 인증된 사용자의 좋아요 상태를 변경합니다.

---

## 7. 댓글 API (`/api/v1/comments`)

### 7.1. 댓글/대댓글 생성
- **Endpoint**: `POST /api/v1/comments`
- **Request**:
  ```json
  {
    "postId": 1,
    "content": "좋은 글이네요!",
    "parentId": null 
  }
  ```
  *(parentId가 null이면 일반 댓글, 존재하면 대댓글)*

---

## 8. 관리자 API (`/api/v1/admin`) - ADMIN 권한 필요

### 8.1. 대시보드 통계
- **Endpoint**: `GET /api/v1/admin/stats`
- **Response**: 회원수, 게시글수, 댓글수 등 집계 데이터 반환.