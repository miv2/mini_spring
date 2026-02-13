# Auth Logout Unit Tests Design (2026-02-13)

## Goal
Add unit tests for logout behavior at the service layer using Mockito, without Spring context.

## Scope
- Target: `AuthService` logout logic
- Verify:
  - access token blacklist registration in Redis
  - refresh token deletion/handling
- Redis and repositories mocked

## Test Cases
1. `logout_blacklists_access_token`
   - Expect: `StringRedisTemplate` sets key `bl:access:{token}` with TTL
2. `logout_deletes_refresh_token`
   - Expect: refresh token repository delete/update invoked

## Test Location & Style
- File: `src/test/java/co/kr/mini_spring/auth/service/AuthServiceLogoutTest.java`
- Mockito-based unit test
- No Spring test annotations

## Verification
- Run: `./gradlew test --tests AuthServiceLogoutTest`
- Expected: PASS with clear failure messages on mock interactions

## Non-Goals
- No controller/HTTP tests
- No integration with real Redis
