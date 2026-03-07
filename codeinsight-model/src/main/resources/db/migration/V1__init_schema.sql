-- CodeInsight Agent - Initial Schema

-- 用户表
CREATE TABLE ci_user (
    id            VARCHAR(36) PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100),
    email         VARCHAR(100),
    role          VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 项目表
CREATE TABLE ci_project (
    id                 VARCHAR(36) PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    description        TEXT,
    source_type        VARCHAR(20)  NOT NULL,
    git_url            VARCHAR(500),
    git_branch         VARCHAR(100) DEFAULT 'main',
    git_credential_id  VARCHAR(36),
    index_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_files        INTEGER      DEFAULT 0,
    total_lines        INTEGER      DEFAULT 0,
    indexed_chunks     INTEGER      DEFAULT 0,
    last_sync_at       TIMESTAMPTZ,
    index_started_at   TIMESTAMPTZ,
    index_completed_at TIMESTAMPTZ,
    owner_id           VARCHAR(36)  REFERENCES ci_user(id),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 会话表
CREATE TABLE ci_conversation (
    id            VARCHAR(36) PRIMARY KEY,
    title         VARCHAR(200),
    project_id    VARCHAR(36)  REFERENCES ci_project(id) ON DELETE CASCADE,
    user_id       VARCHAR(36)  REFERENCES ci_user(id),
    scenario_type VARCHAR(20),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 会话消息表
CREATE TABLE ci_conversation_message (
    id              VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES ci_conversation(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    metadata        JSONB,
    token_count     INTEGER     DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conv_msg_conversation ON ci_conversation_message(conversation_id, created_at);

-- 异步任务表
CREATE TABLE ci_async_task (
    id               VARCHAR(36) PRIMARY KEY,
    task_type        VARCHAR(30) NOT NULL,
    project_id       VARCHAR(36),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress_percent INTEGER     DEFAULT 0,
    progress_message VARCHAR(500),
    params           JSONB,
    error_message    TEXT,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_task_project_status ON ci_async_task(project_id, status);

-- 审计日志表
CREATE TABLE ci_audit_log (
    id            VARCHAR(36) PRIMARY KEY,
    user_id       VARCHAR(36),
    username      VARCHAR(50),
    action        VARCHAR(50)  NOT NULL,
    resource_type VARCHAR(30),
    resource_id   VARCHAR(36),
    details       JSONB,
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user   ON ci_audit_log(user_id, created_at);
CREATE INDEX idx_audit_action ON ci_audit_log(action, created_at);
