SET @exist_ai_chat_conversation_knowledge_column = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_chat_conversation'
      AND column_name = 'knowledge_id'
);
SET @add_ai_chat_conversation_knowledge_column_sql = IF(
    @exist_ai_chat_conversation_knowledge_column = 0,
    'ALTER TABLE `ai_chat_conversation` ADD COLUMN `knowledge_id` bigint NULL COMMENT ''知识库编号'' AFTER `role_id`',
    'SELECT 1'
);
PREPARE add_ai_chat_conversation_knowledge_column_stmt FROM @add_ai_chat_conversation_knowledge_column_sql;
EXECUTE add_ai_chat_conversation_knowledge_column_stmt;
DEALLOCATE PREPARE add_ai_chat_conversation_knowledge_column_stmt;

SET @exist_ai_chat_conversation_knowledge_index = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_chat_conversation'
      AND index_name = 'idx_ai_chat_conversation_knowledge'
);
SET @add_ai_chat_conversation_knowledge_index_sql = IF(
    @exist_ai_chat_conversation_knowledge_index = 0,
    'ALTER TABLE `ai_chat_conversation` ADD INDEX `idx_ai_chat_conversation_knowledge` (`knowledge_id`)',
    'SELECT 1'
);
PREPARE add_ai_chat_conversation_knowledge_index_stmt FROM @add_ai_chat_conversation_knowledge_index_sql;
EXECUTE add_ai_chat_conversation_knowledge_index_stmt;
DEALLOCATE PREPARE add_ai_chat_conversation_knowledge_index_stmt;
