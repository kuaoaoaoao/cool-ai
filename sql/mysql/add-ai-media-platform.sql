-- AI Media Platform phase 1: unified media task + Xiaomi MiMo TTS.

CREATE TABLE IF NOT EXISTS `ai_media_task` (
                                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                               `user_id` bigint NOT NULL COMMENT '用户编号',
                                               `task_type` tinyint NOT NULL COMMENT '任务类型：1=TTS',
                                               `platform` varchar(64) NOT NULL COMMENT '平台',
    `model_id` bigint DEFAULT NULL COMMENT '模型编号',
    `model` varchar(128) NOT NULL COMMENT '模型标识',
    `status` tinyint NOT NULL COMMENT '任务状态',
    `progress` tinyint NOT NULL DEFAULT 0 COMMENT '任务进度 0-100',
    `stage` varchar(64) DEFAULT NULL COMMENT '当前阶段',
    `input_json` json DEFAULT NULL COMMENT '输入参数',
    `output_json` json DEFAULT NULL COMMENT '输出参数',
    `source_file_ids` json DEFAULT NULL COMMENT '源文件编号列表',
    `result_file_ids` json DEFAULT NULL COMMENT '结果文件编号列表',
    `provider_task_id` varchar(128) DEFAULT NULL COMMENT 'Provider 侧任务编号',
    `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
    `error_message` varchar(512) DEFAULT NULL COMMENT '错误信息',
    `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_ai_media_task_user` (`user_id`, `task_type`, `create_time`),
    KEY `idx_ai_media_task_my_page` (`tenant_id`, `deleted`, `user_id`, `task_type`, `id`),
    KEY `idx_ai_media_task_my_order` (`tenant_id`, `deleted`, `user_id`, `create_time`, `id`),
    KEY `idx_ai_media_task_admin_page` (`tenant_id`, `deleted`, `create_time`, `id`),
    KEY `idx_ai_media_task_status` (`status`, `create_time`),
    KEY `idx_ai_media_task_tenant` (`tenant_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 媒体任务';

SET @exist_ai_media_task_my_page_index = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_media_task'
      AND index_name = 'idx_ai_media_task_my_page'
);
SET @add_ai_media_task_my_page_index_sql = IF(
    @exist_ai_media_task_my_page_index = 0,
    'ALTER TABLE `ai_media_task` ADD INDEX `idx_ai_media_task_my_page` (`tenant_id`, `deleted`, `user_id`, `task_type`, `id`)',
    'SELECT 1'
);
PREPARE add_ai_media_task_my_page_index_stmt FROM @add_ai_media_task_my_page_index_sql;
EXECUTE add_ai_media_task_my_page_index_stmt;
DEALLOCATE PREPARE add_ai_media_task_my_page_index_stmt;

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

CREATE TABLE IF NOT EXISTS `ai_media_tool_audit` (
                                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                                     `user_id` bigint DEFAULT NULL COMMENT '用户编号',
                                                     `tool_name` varchar(128) NOT NULL COMMENT '工具名称',
    `task_id` bigint DEFAULT NULL COMMENT '媒体任务编号',
    `success` bit(1) NOT NULL COMMENT '是否成功',
    `reject_reason` varchar(512) DEFAULT NULL COMMENT '拒绝原因',
    `request_json` json DEFAULT NULL COMMENT '请求参数',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_ai_media_tool_audit_user` (`user_id`, `tool_name`, `create_time`),
    KEY `idx_ai_media_tool_audit_task` (`task_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 媒体工具审计';

CREATE TABLE IF NOT EXISTS `ai_voice_profile` (
                                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
                                                  `user_id` bigint NOT NULL COMMENT '用户编号',
                                                  `name` varchar(64) NOT NULL COMMENT '音色名称',
    `source_type` varchar(32) NOT NULL COMMENT '来源类型：BUILTIN/CLONE/DESIGN',
    `platform` varchar(64) NOT NULL COMMENT '平台',
    `model_id` bigint NOT NULL COMMENT '模型编号',
    `model` varchar(128) NOT NULL COMMENT '模型标识',
    `builtin_voice` varchar(128) DEFAULT NULL COMMENT '预置音色',
    `design_prompt` varchar(1024) DEFAULT NULL COMMENT '文字设计音色描述',
    `sample_file_url` varchar(512) DEFAULT NULL COMMENT '克隆样本文件 URL',
    `sample_mime_type` varchar(64) DEFAULT NULL COMMENT '克隆样本 MIME 类型',
    `sample_size` bigint DEFAULT NULL COMMENT '克隆样本大小',
    `consent_confirmed` bit(1) DEFAULT NULL COMMENT '是否确认已获得声音授权',
    `preview_task_id` bigint DEFAULT NULL COMMENT '试听任务编号',
    `status` tinyint NOT NULL COMMENT '状态',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_ai_voice_profile_user` (`tenant_id`, `deleted`, `user_id`, `status`, `create_time`, `id`),
    KEY `idx_ai_voice_profile_preview_task` (`preview_task_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 音色库';

SET @exist_ai_voice_profile_user_index = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ai_voice_profile'
      AND index_name = 'idx_ai_voice_profile_user'
);
SET @add_ai_voice_profile_user_index_sql = IF(
    @exist_ai_voice_profile_user_index = 0,
    'ALTER TABLE `ai_voice_profile` ADD INDEX `idx_ai_voice_profile_user` (`tenant_id`, `deleted`, `user_id`, `status`, `create_time`, `id`)',
    'SELECT 1'
);
PREPARE add_ai_voice_profile_user_index_stmt FROM @add_ai_voice_profile_user_index_sql;
EXECUTE add_ai_voice_profile_user_index_stmt;
DEALLOCATE PREPARE add_ai_voice_profile_user_index_stmt;

INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
VALUES
    (641, 'AI 媒体任务状态', 'ai_media_task_status', 0, '', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (642, 'AI 媒体任务类型', 'ai_media_task_type', 0, '', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (643, 'AI 媒体 TTS 音频格式', 'ai_media_tts_format', 0, '', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (644, 'AI 媒体 TTS 音色', 'ai_media_tts_voice', 0, '由 Provider 配置或部署后按小米 MiMo 可用音色维护', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (645, 'AI 音色来源类型', 'ai_voice_profile_source_type', 0, '', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00')
    ON DUPLICATE KEY UPDATE `update_time` = VALUES(`update_time`);

INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
    (3603, 17, '小米 MiMo', 'XiaomiMiMo', 'ai_platform', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3604, 0, '已创建', '0', 'ai_media_task_status', 0, 'info', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3605, 10, '排队中', '10', 'ai_media_task_status', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3606, 20, '进行中', '20', 'ai_media_task_status', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3607, 30, '已完成', '30', 'ai_media_task_status', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3608, 40, '已失败', '40', 'ai_media_task_status', 0, 'danger', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3609, 50, '已取消', '50', 'ai_media_task_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3610, 60, '重试中', '60', 'ai_media_task_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3611, 0, '未知', '0', 'ai_media_task_type', 0, 'info', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3612, 1, '文本转语音', '1', 'ai_media_task_type', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3613, 1, 'WAV', 'wav', 'ai_media_tts_format', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3614, 2, 'MP3', 'mp3', 'ai_media_tts_format', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3615, 3, 'PCM', 'pcm', 'ai_media_tts_format', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3616, 1, 'MiMo-默认', 'mimo_default', 'ai_media_tts_voice', 0, '', '', '因部署集群而异，中国集群默认为冰糖，其他集群默认为 Mia', '1', NOW(), '1', NOW(), b'0'),
    (3617, 2, '冰糖（中文女声）', '冰糖', 'ai_media_tts_voice', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3618, 3, '茉莉（中文女声）', '茉莉', 'ai_media_tts_voice', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3619, 4, '苏打（中文男声）', '苏打', 'ai_media_tts_voice', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3620, 5, '白桦（中文男声）', '白桦', 'ai_media_tts_voice', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3621, 6, 'Mia（英文女声）', 'Mia', 'ai_media_tts_voice', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3622, 7, 'Chloe（英文女声）', 'Chloe', 'ai_media_tts_voice', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3623, 8, 'Milo（英文男声）', 'Milo', 'ai_media_tts_voice', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3624, 9, 'Dean（英文男声）', 'Dean', 'ai_media_tts_voice', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3625, 1, '预置音色', 'BUILTIN', 'ai_voice_profile_source_type', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3626, 2, '音色克隆', 'CLONE', 'ai_voice_profile_source_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
    (3627, 3, '文字设计音色', 'DESIGN', 'ai_voice_profile_source_type', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0')
    ON DUPLICATE KEY UPDATE `update_time` = VALUES(`update_time`);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
    (6611, 'AI 语音合成', '', 2, 7, 2758, 'media/tts', 'ep:microphone', 'ai/media/tts/index.vue', 'AiMediaTts', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
    (6612, '语音合成创建', 'ai:media:tts:create', 3, 1, 6611, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
    (6613, '媒体任务管理', '', 2, 13, 2760, 'media-task', 'ep:video-play', 'ai/media/manager/index.vue', 'AiMediaManager', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
    (6614, '媒体任务查询', 'ai:media:task:query', 3, 1, 6613, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
    (6615, 'AI 音色库', '', 2, 8, 2758, 'media/voice-profile', 'ep:headset', 'ai/media/voiceProfile/index.vue', 'AiVoiceProfile', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
    (6616, '音色库创建', 'ai:voice-profile:create', 3, 1, 6615, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
    (6617, '音色库修改', 'ai:voice-profile:update', 3, 2, 6615, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
    (6618, '音色库删除', 'ai:voice-profile:delete', 3, 3, 6615, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
    (6619, '音色库试听', 'ai:voice-profile:preview', 3, 4, 6615, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
    ON DUPLICATE KEY UPDATE `update_time` = VALUES(`update_time`);
