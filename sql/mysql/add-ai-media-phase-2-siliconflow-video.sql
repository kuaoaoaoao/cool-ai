-- AI 媒体平台 Phase 2：SiliconFlow 图生视频 MVP

-- 任务类型字典：图生视频、文生视频预留
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 30, '图生视频', '2', 'ai_media_task_type', 0, 'primary', '', 'SiliconFlow IMAGE_TO_VIDEO', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data WHERE dict_type = 'ai_media_task_type' AND value = '2' AND deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 40, '文生视频', '3', 'ai_media_task_type', 0, 'info', '', 'Phase 2 预留，MVP 默认不启用', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data WHERE dict_type = 'ai_media_task_type' AND value = '3' AND deleted = b'0'
);

-- 任务状态字典：补充已提交、取消中
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 15, '已提交', '15', 'ai_media_task_status', 0, 'primary', '', 'Provider 已接受任务', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data WHERE dict_type = 'ai_media_task_status' AND value = '15' AND deleted = b'0'
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 45, '取消中', '45', 'ai_media_task_status', 0, 'warning', '', '任务正在取消', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data WHERE dict_type = 'ai_media_task_status' AND value = '45' AND deleted = b'0'
);

-- 前端菜单：图生视频页面挂到 AI 生成功能分组下，和“AI 语音合成”同级。
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status,
                         visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT 'AI 视频生成', '', 2, 9, parent_menu.id, 'media/video', 'ep:video-camera', 'ai/media/video/index.vue',
       'AiMediaVideo', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT id FROM system_menu WHERE id = 2758 AND deleted = b'0' LIMIT 1) parent_menu
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu m
    WHERE (m.name = 'AI 视频生成' OR m.component_name = 'AiMediaVideo' OR m.path = 'media/video')
      AND m.deleted = b'0'
)
LIMIT 1;

-- 后端权限：图生视频创建。
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status,
                         visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '图生视频创建', 'ai:media:video:create', 3, 1, COALESCE(video_menu.id, task_menu.id), '', '', '', '', 0,
       b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) seed
LEFT JOIN (SELECT id FROM system_menu WHERE name = 'AI 视频生成' AND deleted = b'0' LIMIT 1) video_menu ON 1 = 1
LEFT JOIN (SELECT id FROM system_menu WHERE name = '媒体任务管理' AND deleted = b'0' LIMIT 1) task_menu ON 1 = 1
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu m WHERE m.permission = 'ai:media:video:create' AND m.deleted = b'0'
)
  AND COALESCE(video_menu.id, task_menu.id) IS NOT NULL
LIMIT 1;

-- 后端权限：媒体资产聚合查询。
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status,
                         visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '媒体资产查询', 'ai:media:asset:query', 3, 2, task_menu.id, '', '', '', '', 0,
       b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT id FROM system_menu WHERE name = '媒体任务管理' AND deleted = b'0' LIMIT 1) task_menu
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu m WHERE m.permission = 'ai:media:asset:query' AND m.deleted = b'0'
)
LIMIT 1;

-- 后端权限：敏感媒体能力。默认不暴露菜单，仅作为服务端安全闸门使用。
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status,
                         visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '人物替换创建', 'ai:media:person-replacement:create', 3, 3, task_menu.id, '', '', '', '', 0,
       b'0', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT id FROM system_menu WHERE name = '媒体任务管理' AND deleted = b'0' LIMIT 1) task_menu
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu m WHERE m.permission = 'ai:media:person-replacement:create' AND m.deleted = b'0'
)
LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status,
                         visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '本地声音克隆创建', 'ai:media:voice-clone:create', 3, 4, task_menu.id, '', '', '', '', 0,
       b'0', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT id FROM system_menu WHERE name = '媒体任务管理' AND deleted = b'0' LIMIT 1) task_menu
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu m WHERE m.permission = 'ai:media:voice-clone:create' AND m.deleted = b'0'
)
LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status,
                         visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '本地语音转换创建', 'ai:media:voice-conversion:create', 3, 5, task_menu.id, '', '', '', '', 0,
       b'0', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT id FROM system_menu WHERE name = '媒体任务管理' AND deleted = b'0' LIMIT 1) task_menu
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu m WHERE m.permission = 'ai:media:voice-conversion:create' AND m.deleted = b'0'
)
LIMIT 1;

-- 推荐配置：创建 SiliconFlow 视频模型时使用 VIDEO 类型，模型标识可先配置为 Wan-AI/Wan2.2-I2V-A14B。
-- 推荐 Job：在 XXL-Job 中添加 aiSiliconFlowVideoSyncJob，按 30-60 秒间隔调度。
-- 可选 Worker Job：在 XXL-Job 中添加 aiPythonWorkerMediaSyncJob，按 30-60 秒间隔调度。
