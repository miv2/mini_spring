# Post Create ProfileImage Fetch Fix Design (2026-02-13)

## Goal
Prevent LazyInitializationException when creating posts by loading the author's profile image in-session.

## Scope
- `PostService.createPost` only
- `SocialMemberRepository` fetch join by email

## Design
- Add repository method: `findByEmailWithProfileImage(String email)` using fetch join
- In `PostService.createPost`, re-fetch the author by email and use it when building `PostResponse`
- Keep existing `member` for auth/identity but response uses the fetched entity

## Testing
- Unit test ensures `createPost` returns `profileImageUrl` correctly without triggering LazyInitializationException

## Non-Goals
- No schema changes
- No changes to list/detail endpoints
