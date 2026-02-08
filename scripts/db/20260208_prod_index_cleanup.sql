-- Production DDL: remove duplicate indexes (safe, minimal)
-- Target: app_server

ALTER TABLE member
    DROP INDEX idx_email,
    DROP INDEX idx_nickname;

ALTER TABLE hashtag
    DROP INDEX idx_name;
