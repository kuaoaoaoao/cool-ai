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
  KEY `idx_ai_media_task_status` (`status`, `create_time`),
  KEY `idx_ai_media_task_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 媒体任务';

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

INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
VALUES
  (641, 'AI 媒体任务状态', 'ai_media_task_status', 0, '', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
  (642, 'AI 媒体任务类型', 'ai_media_task_type', 0, '', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
  (643, 'AI 媒体 TTS 音频格式', 'ai_media_tts_format', 0, '', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
  (644, 'AI 媒体 TTS 音色', 'ai_media_tts_voice', 0, '由 Provider 配置或部署后按小米 MiMo 可用音色维护', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00')
ON DUPLICATE KEY UPDATE `update_time` = VALUES(`update_time`);

INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (3031, 17, '小米 MiMo', 'XiaomiMiMo', 'ai_platform', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3032, 0, '已创建', '0', 'ai_media_task_status', 0, 'info', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3033, 10, '排队中', '10', 'ai_media_task_status', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3034, 20, '进行中', '20', 'ai_media_task_status', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3035, 30, '已完成', '30', 'ai_media_task_status', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3036, 40, '已失败', '40', 'ai_media_task_status', 0, 'danger', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3037, 50, '已取消', '50', 'ai_media_task_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3038, 60, '重试中', '60', 'ai_media_task_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3039, 0, '未知', '0', 'ai_media_task_type', 0, 'info', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3040, 1, '文本转语音', '1', 'ai_media_task_type', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3041, 1, 'MP3', 'mp3', 'ai_media_tts_format', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3042, 2, 'WAV', 'wav', 'ai_media_tts_format', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'),
  (3043, 3, 'PCM', 'pcm', 'ai_media_tts_format', 0, '', '', '', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE `update_time` = VALUES(`update_time`);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (5010, 'AI 语音合成', '', 2, 7, 2758, 'media/tts', 'ep:microphone', 'ai/media/tts/index.vue', 'AiMediaTts', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (5011, '语音合成创建', 'ai:media:tts:create', 3, 1, 5010, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (5012, '媒体任务管理', '', 2, 13, 2760, 'media-task', 'ep:video-play', 'ai/media/manager/index.vue', 'AiMediaManager', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (5013, '媒体任务查询', 'ai:media:task:query', 3, 1, 5012, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE `update_time` = VALUES(`update_time`);
