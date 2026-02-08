ALTER TABLE member
    DROP INDEX oauth_id,
    DROP INDEX idx_oauth_lookup,
    ADD UNIQUE KEY uk_oauth_provider_id (oauth_provider, oauth_id);

ALTER TABLE post_view
    MODIFY view_count INT DEFAULT 1;
