-- Fix AI media task page sorting on existing databases.
-- The admin page queries by tenant/deleted and orders by create_time/id desc.
-- The "my task" page also orders by create_time/id desc after filtering by user.

SET @exist_ai_media_task_my_order_index = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_media_task'
      AND index_name = 'idx_ai_media_task_my_order'
);
SET @add_ai_media_task_my_order_index_sql = IF(
    @exist_ai_media_task_my_order_index = 0,
    'ALTER TABLE `ai_media_task` ADD INDEX `idx_ai_media_task_my_order` (`tenant_id`, `deleted`, `user_id`, `create_time`, `id`)',
    'SELECT 1'
);
PREPARE add_ai_media_task_my_order_index_stmt FROM @add_ai_media_task_my_order_index_sql;
EXECUTE add_ai_media_task_my_order_index_stmt;
DEALLOCATE PREPARE add_ai_media_task_my_order_index_stmt;

SET @exist_ai_media_task_admin_page_index = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_media_task'
      AND index_name = 'idx_ai_media_task_admin_page'
);
SET @add_ai_media_task_admin_page_index_sql = IF(
    @exist_ai_media_task_admin_page_index = 0,
    'ALTER TABLE `ai_media_task` ADD INDEX `idx_ai_media_task_admin_page` (`tenant_id`, `deleted`, `create_time`, `id`)',
    'SELECT 1'
);
PREPARE add_ai_media_task_admin_page_index_stmt FROM @add_ai_media_task_admin_page_index_sql;
EXECUTE add_ai_media_task_admin_page_index_stmt;
DEALLOCATE PREPARE add_ai_media_task_admin_page_index_stmt;
