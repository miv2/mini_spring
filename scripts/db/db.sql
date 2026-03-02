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

CREATE TABLE IF NOT EXISTS refresh_token (
  id BIGSERIAL PRIMARY KEY,
  author_id BIGINT NOT NULL,
  token VARCHAR(255) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP(3) NOT NULL,
  is_revoked BOOLEAN DEFAULT FALSE,
  CONSTRAINT uk_refresh_token UNIQUE (token)
);

CREATE TABLE IF NOT EXISTS conversations (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(10) NOT NULL,
  unique_key VARCHAR(100) UNIQUE,
  owner_id BIGINT NOT NULL,
  title VARCHAR(100),
  last_message_at TIMESTAMP(6),
  last_message_preview VARCHAR(255),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP(6),
  CONSTRAINT chk_conversation_type CHECK (type IN ('DIRECT', 'GROUP')),
  CONSTRAINT fk_conversations_owner FOREIGN KEY (owner_id) REFERENCES social_member (id)
);

CREATE TABLE IF NOT EXISTS conversation_participants (
  id BIGSERIAL PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  last_read_message_id BIGINT,
  joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP(6),
  CONSTRAINT fk_participants_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
  CONSTRAINT fk_participants_user FOREIGN KEY (user_id) REFERENCES social_member (id)
);

CREATE TABLE IF NOT EXISTS messages (
  id BIGSERIAL PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  client_message_id VARCHAR(100) NOT NULL,
  type VARCHAR(10) NOT NULL DEFAULT 'TEXT',
  content TEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP(6),
  CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES social_member (id),
  CONSTRAINT chk_message_type CHECK (type IN ('TEXT', 'IMAGE', 'SYSTEM'))
);

CREATE TABLE IF NOT EXISTS user_blocks (
  id BIGSERIAL PRIMARY KEY,
  blocker_id BIGINT NOT NULL,
  blocked_id BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES social_member (id),
  CONSTRAINT fk_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES social_member (id),
  CONSTRAINT chk_user_blocks_self CHECK (blocker_id <> blocked_id)
);

CREATE TABLE IF NOT EXISTS conversation_bans (
  id BIGSERIAL PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  banned_by BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_conversation_bans_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
  CONSTRAINT fk_conversation_bans_user FOREIGN KEY (user_id) REFERENCES social_member (id),
  CONSTRAINT fk_conversation_bans_banned_by FOREIGN KEY (banned_by) REFERENCES social_member (id)
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

CREATE INDEX IF NOT EXISTS idx_member_id ON refresh_token (author_id);
CREATE INDEX IF NOT EXISTS idx_expires_at ON refresh_token (expires_at);
CREATE INDEX IF NOT EXISTS idx_member_revoked ON refresh_token (author_id, is_revoked);

CREATE INDEX IF NOT EXISTS idx_conversations_last_message_at ON conversations (last_message_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_participants_conv_user_active
  ON conversation_participants (conversation_id, user_id)
  WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_participants_user_id ON conversation_participants (user_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_participants_conv_id ON conversation_participants (conversation_id, deleted_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_messages_conv_client_id ON messages (conversation_id, client_message_id);
CREATE INDEX IF NOT EXISTS idx_messages_conv_id_desc ON messages (conversation_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages (sender_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_blocks_blocker_blocked ON user_blocks (blocker_id, blocked_id);
CREATE INDEX IF NOT EXISTS idx_blocks_blocker_id ON user_blocks (blocker_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_conversation_bans_conv_user ON conversation_bans (conversation_id, user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_bans_user_id ON conversation_bans (user_id);
