# Post Detail isLiked Design (2026-02-13)

## Goal
Add `isLiked` to post detail response based on the logged-in user's like state.

## Scope
- Endpoint: `GET /api/posts/{id}` only
- Logged-in users only (non-authenticated users cannot access)

## Design
- Service layer (`PostService.getPost`) queries like existence for the current user:
  - `postLikeRepository.findLike(memberId, postId)`
  - `isLiked = true` if present, else `false`
- DTO layer (`PostResponse`) adds boolean field `isLiked`
- Keep `isOwner` unchanged

## Query Impact
- Adds 1 existence query on detail read
- Acceptable for current scale; can optimize later with join/projection if needed

## Testing
- Unit tests verify:
  - liked -> `isLiked=true`
  - not liked -> `isLiked=false`

## Non-Goals
- No changes to list responses
- No changes for anonymous users
