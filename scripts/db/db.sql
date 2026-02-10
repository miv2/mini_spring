CREATE TABLE IF NOT EXISTS file (
  id BIGSERIAL PRIMARY KEY,
  origin_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(255) NOT NULL,
  file_size BIGINT NOT NULL,
  extension VARCHAR(10) NOT NULL,
  content_type VARCHAR(100),
  type VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_stored_name UNIQUE (stored_name)
);

CREATE TABLE IF NOT EXISTS social_member (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  provider VARCHAR(20) NOT NULL,
  oauth_id VARCHAR(255) NOT NULL,
  name VARCHAR(100) NOT NULL,
  nickname VARCHAR(100) NOT NULL,
  profile_image_id BIGINT,
  role VARCHAR(10) NOT NULL DEFAULT 'USER',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_login_at TIMESTAMP(6),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP(6),
  CONSTRAINT uk_social_member_email UNIQUE (email),
  CONSTRAINT uk_social_member_nickname UNIQUE (nickname),
  CONSTRAINT uk_provider_oauth_id UNIQUE (provider, oauth_id)
);

CREATE TABLE IF NOT EXISTS post (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  view_count INTEGER DEFAULT 0,
  like_count INTEGER DEFAULT 0,
  comment_count INTEGER DEFAULT 0,
  is_published BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP(6),
  author_id BIGINT
);

CREATE TABLE IF NOT EXISTS comment (
  id BIGSERIAL PRIMARY KEY,
  content TEXT NOT NULL,
  post_id BIGINT NOT NULL,
  parent_comment_id BIGINT,
  depth INTEGER NOT NULL,
  is_deleted BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP(6),
  author_id BIGINT,
  CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES comment (id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
  CONSTRAINT chk_depth_range CHECK (depth BETWEEN 0 AND 1),
  CONSTRAINT chk_parent_depth CHECK (
    (parent_comment_id IS NULL AND depth = 0) OR
    (parent_comment_id IS NOT NULL AND depth = 1)
  )
);

CREATE TABLE IF NOT EXISTS hashtag (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  usage_count INTEGER DEFAULT 0,
  created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  last_used_at TIMESTAMP(3),
  CONSTRAINT uk_hashtag_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS post_hashtag (
  post_id BIGINT NOT NULL,
  hashtag_id BIGINT NOT NULL,
  created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, hashtag_id),
  CONSTRAINT fk_post_hashtag_hashtag FOREIGN KEY (hashtag_id) REFERENCES hashtag (id) ON DELETE CASCADE,
  CONSTRAINT fk_post_hashtag_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS post_like (
  author_id BIGINT NOT NULL,
  post_id BIGINT NOT NULL,
  created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (author_id, post_id),
  CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS post_view (
  last_viewed_at TIMESTAMP(6) NOT NULL,
  view_count INTEGER DEFAULT 1,
  post_id BIGINT NOT NULL,
  CONSTRAINT fk_post_view_post FOREIGN KEY (post_id) REFERENCES post (id)
);

CREATE TABLE IF NOT EXISTS refresh_token (
  id BIGSERIAL PRIMARY KEY,
  author_id BIGINT NOT NULL,
  token VARCHAR(255) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP(3) NOT NULL,
  is_revoked BOOLEAN DEFAULT FALSE,
  CONSTRAINT uk_refresh_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_post_created ON comment (post_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_member_created ON comment (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_parent_depth ON comment (parent_comment_id, depth);
CREATE INDEX IF NOT EXISTS idx_deleted_at ON comment (deleted_at);

CREATE INDEX IF NOT EXISTS idx_status_created ON social_member (status, created_at);
CREATE INDEX IF NOT EXISTS idx_email ON social_member (email);
CREATE INDEX IF NOT EXISTS idx_nickname ON social_member (nickname);

CREATE INDEX IF NOT EXISTS idx_popularity ON hashtag (usage_count DESC, last_used_at DESC);
CREATE INDEX IF NOT EXISTS idx_recent ON hashtag (last_used_at DESC);

CREATE INDEX IF NOT EXISTS idx_author_id ON post (author_id);
CREATE INDEX IF NOT EXISTS idx_created_at ON post (created_at);
CREATE INDEX IF NOT EXISTS idx_deleted_at_post ON post (deleted_at);
CREATE INDEX IF NOT EXISTS idx_published_created ON post (is_published, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_published_like ON post (is_published, like_count DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_hashtag_id ON post_hashtag (hashtag_id);
CREATE INDEX IF NOT EXISTS idx_post_id ON post_like (post_id);

CREATE INDEX IF NOT EXISTS idx_last_viewed_at ON post_view (last_viewed_at);
CREATE INDEX IF NOT EXISTS idx_post_view_post_id ON post_view (post_id);

CREATE INDEX IF NOT EXISTS idx_member_id ON refresh_token (author_id);
CREATE INDEX IF NOT EXISTS idx_expires_at ON refresh_token (expires_at);
CREATE INDEX IF NOT EXISTS idx_member_revoked ON refresh_token (author_id, is_revoked);
