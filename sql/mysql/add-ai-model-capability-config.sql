-- AI model registry upgrade: explicit capabilities, modalities, options, limits, metadata and scene defaults.

SET @exist_ai_model_capabilities_column = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_model'
      AND column_name = 'capabilities'
);
SET @add_ai_model_capabilities_column_sql = IF(
    @exist_ai_model_capabilities_column = 0,
    'ALTER TABLE `ai_model` ADD COLUMN `capabilities` json DEFAULT NULL COMMENT ''能力标签'' AFTER `status`',
    'SELECT 1'
);
PREPARE add_ai_model_capabilities_column_stmt FROM @add_ai_model_capabilities_column_sql;
EXECUTE add_ai_model_capabilities_column_stmt;
DEALLOCATE PREPARE add_ai_model_capabilities_column_stmt;

SET @exist_ai_model_input_modalities_column = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_model'
      AND column_name = 'input_modalities'
);
SET @add_ai_model_input_modalities_column_sql = IF(
    @exist_ai_model_input_modalities_column = 0,
    'ALTER TABLE `ai_model` ADD COLUMN `input_modalities` json DEFAULT NULL COMMENT ''输入模态'' AFTER `capabilities`',
    'SELECT 1'
);
PREPARE add_ai_model_input_modalities_column_stmt FROM @add_ai_model_input_modalities_column_sql;
EXECUTE add_ai_model_input_modalities_column_stmt;
DEALLOCATE PREPARE add_ai_model_input_modalities_column_stmt;

SET @exist_ai_model_output_modalities_column = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_model'
      AND column_name = 'output_modalities'
);
SET @add_ai_model_output_modalities_column_sql = IF(
    @exist_ai_model_output_modalities_column = 0,
    'ALTER TABLE `ai_model` ADD COLUMN `output_modalities` json DEFAULT NULL COMMENT ''输出模态'' AFTER `input_modalities`',
    'SELECT 1'
);
PREPARE add_ai_model_output_modalities_column_stmt FROM @add_ai_model_output_modalities_column_sql;
EXECUTE add_ai_model_output_modalities_column_stmt;
DEALLOCATE PREPARE add_ai_model_output_modalities_column_stmt;

SET @exist_ai_model_options_column = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_model'
      AND column_name = 'options'
);
SET @add_ai_model_options_column_sql = IF(
    @exist_ai_model_options_column = 0,
    'ALTER TABLE `ai_model` ADD COLUMN `options` json DEFAULT NULL COMMENT ''模型默认参数'' AFTER `output_modalities`',
    'SELECT 1'
);
PREPARE add_ai_model_options_column_stmt FROM @add_ai_model_options_column_sql;
EXECUTE add_ai_model_options_column_stmt;
DEALLOCATE PREPARE add_ai_model_options_column_stmt;

SET @exist_ai_model_limits_column = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_model'
      AND column_name = 'limits'
);
SET @add_ai_model_limits_column_sql = IF(
    @exist_ai_model_limits_column = 0,
    'ALTER TABLE `ai_model` ADD COLUMN `limits` json DEFAULT NULL COMMENT ''模型限制信息'' AFTER `options`',
    'SELECT 1'
);
PREPARE add_ai_model_limits_column_stmt FROM @add_ai_model_limits_column_sql;
EXECUTE add_ai_model_limits_column_stmt;
DEALLOCATE PREPARE add_ai_model_limits_column_stmt;

SET @exist_ai_model_metadata_column = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_model'
      AND column_name = 'metadata'
);
SET @add_ai_model_metadata_column_sql = IF(
    @exist_ai_model_metadata_column = 0,
    'ALTER TABLE `ai_model` ADD COLUMN `metadata` json DEFAULT NULL COMMENT ''扩展元数据'' AFTER `limits`',
    'SELECT 1'
);
PREPARE add_ai_model_metadata_column_stmt FROM @add_ai_model_metadata_column_sql;
EXECUTE add_ai_model_metadata_column_stmt;
DEALLOCATE PREPARE add_ai_model_metadata_column_stmt;

CREATE TABLE IF NOT EXISTS `ai_model_scene` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `scene` varchar(64) NOT NULL COMMENT '场景编码',
    `model_id` bigint NOT NULL COMMENT '模型编号',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint NOT NULL COMMENT '状态',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_ai_model_scene_scene` (`tenant_id`, `deleted`, `scene`, `status`, `sort`, `id`),
    KEY `idx_ai_model_scene_model` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型场景默认配置';
