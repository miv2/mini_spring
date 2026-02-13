# Post Profile Image URL Design (2026-02-13)

## Goal
Include `profileImageUrl` in post list and detail responses, using the configured default image when none exists.

## Scope
- Responses:
  - `PostSummaryResponse` (list)
  - `PostResponse` (detail)
- Logged-in users only for detail (as per current access rules)

## Design
- Inject `file.default-profile-image` into `PostService`
- When building responses:
  - If author exists: `author.getProfileImageUrl(defaultProfileImage)`
  - If author is null: use `defaultProfileImage`
- Add `profileImageUrl` field to DTOs and include in JSON order

## Rationale
- Aligns with existing `MemberResponse` behavior
- Minimizes changes and avoids extra DB queries

## Testing
- Unit tests for list/detail verify:
  - real profile image URL is used when present
  - default image URL is used when missing

## Non-Goals
- No schema changes
- No list/detail endpoint changes beyond adding the field
