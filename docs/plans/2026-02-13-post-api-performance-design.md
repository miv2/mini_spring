# Post API Performance Design (2026-02-13)

## Goal
Improve response time for post list and detail APIs with acceptable data freshness of tens of seconds to a few minutes.

## Scope
- Endpoints: `GET /api/posts`, `GET /api/posts/{id}`
- Read performance only; no changes to API shape.
- Redis caching allowed.

## Approach (Recommended)
**Read-through cache with TTL + cache invalidation on writes.**

### Data Flow
1. Read endpoint checks Redis cache by key.
2. On miss, load from DB, build response, store in Redis with TTL.
3. On hit, return cached response.
4. On writes (create/update/delete/like/comment), invalidate relevant keys.

### Cache Keys
- List: `posts:list:{page}:{size}:{sort}:{keyword}:{hashtags}:{authorId}`
- Detail: `posts:detail:{postId}`

### TTL Policy
- List: 30–120 seconds
- Detail: 60–300 seconds

### Invalidation Policy
- Create/Update/Delete:
  - Delete `posts:detail:{id}` if applicable
  - Invalidate list caches (pattern delete or namespace versioning)
- Like/Comment count changes:
  - Delete detail cache
  - Invalidate list caches if counts are included

### Error Handling
- Redis failure: fall back to DB
- Cache set failure: do not fail request
- Cache staleness accepted within TTL window

## Testing/Verification
- Cache miss vs hit response equality
- Write actions trigger invalidation
- SQL log confirms reduced query volume on repeated reads

## Non-Goals
- No schema changes
- No API contract changes
- No background prewarming
