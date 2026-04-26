-- Spring AI Chat Memory table (required by JdbcChatMemoryRepository)
CREATE TABLE spring_ai_chat_memory (
    conversation_id VARCHAR(36) NOT NULL,
    content         TEXT        NOT NULL,
    type            VARCHAR(20) NOT NULL,
    "timestamp"     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_memory_conv ON spring_ai_chat_memory(conversation_id, "timestamp");
