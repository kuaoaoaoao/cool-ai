package cn.iocoder.yudao.module.ai.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * AI 错误码枚举类
 * <p>
 * ai 系统，使用 1-040-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== API 密钥 1-040-000-000 ==========
    ErrorCode API_KEY_NOT_EXISTS = new ErrorCode(1_040_000_000, "API 密钥不存在");
    ErrorCode API_KEY_DISABLE = new ErrorCode(1_040_000_001, "API 密钥已禁用！");

    // ========== API 模型 1-040-001-000 ==========
    ErrorCode MODEL_NOT_EXISTS = new ErrorCode(1_040_001_000, "模型不存在!");
    ErrorCode MODEL_DISABLE = new ErrorCode(1_040_001_001, "模型({})已禁用!");
    ErrorCode MODEL_DEFAULT_NOT_EXISTS = new ErrorCode(1_040_001_002, "操作失败，找不到默认模型");
    ErrorCode MODEL_USE_TYPE_ERROR = new ErrorCode(1_040_001_003, "操作失败，该模型的模型类型不正确");
    ErrorCode MODEL_PLATFORM_KEY_NOT_MATCH = new ErrorCode(1_040_001_004, "操作失败，模型平台({})和 API 密钥平台({})不一致");

    // ========== API 聊天角色 1-040-002-000 ==========
    ErrorCode CHAT_ROLE_NOT_EXISTS = new ErrorCode(1_040_002_000, "聊天角色不存在");
    ErrorCode CHAT_ROLE_DISABLE = new ErrorCode(1_040_001_001, "聊天角色({})已禁用!");

    // ========== API 聊天会话 1-040-003-000 ==========
    ErrorCode CHAT_CONVERSATION_NOT_EXISTS = new ErrorCode(1_040_003_000, "对话不存在!");
    ErrorCode CHAT_CONVERSATION_MODEL_ERROR = new ErrorCode(1_040_003_001, "操作失败，该聊天模型的配置不完整");

    // ========== API 聊天消息 1-040-004-000 ==========
    ErrorCode CHAT_MESSAGE_NOT_EXIST = new ErrorCode(1_040_004_000, "消息不存在!");
    ErrorCode CHAT_STREAM_ERROR = new ErrorCode(1_040_004_001, "对话生成异常!");
    ErrorCode CHAT_MODEL_NOT_SUPPORT_IMAGE = new ErrorCode(1_040_004_002, "当前模型不支持图片理解，请切换多模态模型");
    ErrorCode CHAT_WEB_SEARCH_NOT_ENABLE = new ErrorCode(1_040_004_003, "联网搜索未启用，请先配置搜索服务");
    ErrorCode CHAT_MCP_NOT_ENABLE = new ErrorCode(1_040_004_004, "MCP 客户端未启用，请先配置 MCP 服务");
    ErrorCode CHAT_MCP_CLIENT_NOT_EXISTS = new ErrorCode(1_040_004_005, "MCP 客户端({})不存在或未连接");
    ErrorCode CHAT_IMAGE_DOWNLOAD_FAIL = new ErrorCode(1_040_004_006, "图片附件下载失败：{}");
    ErrorCode CHAT_MODEL_NOT_SUPPORT_AUDIO = new ErrorCode(1_040_004_007, "当前模型不支持音频理解，请切换多模态模型");
    ErrorCode CHAT_MODEL_NOT_SUPPORT_VIDEO = new ErrorCode(1_040_004_008, "当前模型不支持视频理解，请切换多模态模型");
    ErrorCode CHAT_MULTIMODAL_ATTACHMENT_FAIL = new ErrorCode(1_040_004_009, "多模态附件处理失败：{}");

    // ========== API 绘画 1-040-005-000 ==========
    ErrorCode IMAGE_NOT_EXISTS = new ErrorCode(1_040_005_000, "图片不存在!");
    ErrorCode IMAGE_MIDJOURNEY_SUBMIT_FAIL = new ErrorCode(1_040_005_001, "Midjourney 提交失败!原因：{}");
    ErrorCode IMAGE_CUSTOM_ID_NOT_EXISTS = new ErrorCode(1_040_005_002, "Midjourney 按钮 customId 不存在! {}");
    ErrorCode IMAGE_GENERATE_RESULT_EMPTY = new ErrorCode(1_040_005_003, "图片生成结果为空，请检查模型或服务商响应");

    // ========== API 音乐 1-040-006-000 ==========
    ErrorCode MUSIC_NOT_EXISTS = new ErrorCode(1_040_006_000, "音乐不存在!");
    ErrorCode MUSIC_GENERATE_PARAM_ERROR = new ErrorCode(1_040_006_001, "音乐生成参数错误：{}");
    ErrorCode MUSIC_GENERATE_RESPONSE_EMPTY = new ErrorCode(1_040_006_002, "音乐生成提交失败：Suno 未返回任务");

    // ========== API 写作 1-040-007-000 ==========
    ErrorCode WRITE_NOT_EXISTS = new ErrorCode(1_040_007_000, "作文不存在!");
    ErrorCode WRITE_STREAM_ERROR = new ErrorCode(1_040_007_001, "写作生成异常!");
    ErrorCode WRITE_ORIGINAL_CONTENT_REQUIRED = new ErrorCode(1_040_007_002, "回复写作时，原文不能为空");
    ErrorCode WRITE_GENERATE_RESULT_EMPTY = new ErrorCode(1_040_007_003, "写作生成结果为空，请检查模型或服务商响应");

    // ========== API 思维导图 1-040-008-000 ==========
    ErrorCode MIND_MAP_NOT_EXISTS = new ErrorCode(1_040_008_000, "思维导图不存在!");
    ErrorCode MIND_MAP_STREAM_ERROR = new ErrorCode(1_040_008_001, "思维导图生成异常!");
    ErrorCode MIND_MAP_GENERATE_RESULT_EMPTY = new ErrorCode(1_040_008_002, "思维导图生成结果为空，请检查模型或服务商响应");

    // ========== API 知识库 1-040-009-000 ==========
    ErrorCode KNOWLEDGE_NOT_EXISTS = new ErrorCode(1_040_009_000, "知识库不存在!");
    ErrorCode KNOWLEDGE_DISABLE = new ErrorCode(1_040_009_001, "知识库({})已禁用");

    ErrorCode KNOWLEDGE_DOCUMENT_NOT_EXISTS = new ErrorCode(1_040_009_101, "文档不存在!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_EMPTY = new ErrorCode(1_040_009_102, "文档内容为空!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_DOWNLOAD_FAIL = new ErrorCode(1_040_009_102, "文件下载失败!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_READ_FAIL = new ErrorCode(1_040_009_102, "文档加载失败!");

    ErrorCode KNOWLEDGE_SEGMENT_NOT_EXISTS = new ErrorCode(1_040_009_202, "段落不存在!");
    ErrorCode KNOWLEDGE_SEGMENT_CONTENT_TOO_LONG = new ErrorCode(1_040_009_203, "内容 Token 数为 {}，超过最大限制 {}");

    // ========== AI 工具 1-040-010-000 ==========
    ErrorCode TOOL_NOT_EXISTS = new ErrorCode(1_040_010_000, "工具不存在");
    ErrorCode TOOL_NAME_NOT_EXISTS = new ErrorCode(1_040_010_001, "工具({})找不到 Bean");
    ErrorCode TOOL_DISABLE = new ErrorCode(1_040_010_002, "工具({})已禁用");

    // ========== AI 工作流 1-040-011-000 ==========
    ErrorCode WORKFLOW_NOT_EXISTS = new ErrorCode(1_040_011_000, "工作流不存在");
    ErrorCode WORKFLOW_CODE_EXISTS = new ErrorCode(1_040_011_001, "工作流标识已存在");

    // ========== AI 媒体任务 1-040-012-000 ==========
    ErrorCode MEDIA_TASK_NOT_EXISTS = new ErrorCode(1_040_012_000, "媒体任务不存在");
    ErrorCode MEDIA_TASK_FORBIDDEN = new ErrorCode(1_040_012_001, "无权访问该媒体任务");
    ErrorCode MEDIA_TASK_TYPE_NOT_ENABLE = new ErrorCode(1_040_012_002, "媒体能力({})暂未启用");
    ErrorCode MEDIA_TTS_DISABLED = new ErrorCode(1_040_012_003, "TTS 生成功能未开启");
    ErrorCode MEDIA_TTS_QUOTA_EXCEEDED = new ErrorCode(1_040_012_004, "今日 TTS 生成次数已达上限");
    ErrorCode MEDIA_PROVIDER_NOT_EXISTS = new ErrorCode(1_040_012_005, "未找到可用的媒体 Provider");
    ErrorCode MEDIA_PROVIDER_CONFIG_MISSING = new ErrorCode(1_040_012_006, "媒体 Provider 配置缺失：{}");
    ErrorCode MEDIA_PROVIDER_CALL_FAIL = new ErrorCode(1_040_012_007, "媒体 Provider 调用失败：{}");
    ErrorCode MEDIA_PROVIDER_RESPONSE_INVALID = new ErrorCode(1_040_012_008, "媒体 Provider 返回格式异常");
    ErrorCode MEDIA_FILE_SAVE_FAIL = new ErrorCode(1_040_012_009, "媒体结果文件保存失败：{}");
    ErrorCode MEDIA_TTS_QUOTA_LOCK_FAIL = new ErrorCode(1_040_012_010, "TTS 配额校验繁忙，请稍后重试");
    ErrorCode MEDIA_VIDEO_DISABLED = new ErrorCode(1_040_012_011, "视频生成功能未开启");
    ErrorCode MEDIA_VIDEO_QUOTA_EXCEEDED = new ErrorCode(1_040_012_012, "今日视频生成次数已达上限");
    ErrorCode MEDIA_VIDEO_SOURCE_REQUIRED = new ErrorCode(1_040_012_013, "图生视频源图片不能为空");
    ErrorCode MEDIA_VIDEO_SOURCE_INVALID = new ErrorCode(1_040_012_014, "图生视频源图片不合法：{}");
    ErrorCode MEDIA_VIDEO_SIZE_INVALID = new ErrorCode(1_040_012_015, "视频尺寸不支持：{}");
    ErrorCode MEDIA_VIDEO_CONCURRENT_EXCEEDED = new ErrorCode(1_040_012_016, "当前运行中的视频任务已达上限");
    ErrorCode MEDIA_VIDEO_MODEL_CAPABILITY_UNSUPPORTED = new ErrorCode(1_040_012_017, "当前视频模型不支持图生视频，请选择 image-to-video 模型");
    ErrorCode MEDIA_VIDEO_SOURCE_FORBIDDEN = new ErrorCode(1_040_012_018, "无权使用该图生视频源图片");
    ErrorCode MEDIA_VIDEO_TENANT_QUOTA_EXCEEDED = new ErrorCode(1_040_012_019, "当前租户今日视频生成次数已达上限");
    ErrorCode MEDIA_VIDEO_DURATION_INVALID = new ErrorCode(1_040_012_020, "视频时长不支持：{}");
    ErrorCode MEDIA_TASK_STATUS_INVALID = new ErrorCode(1_040_012_021, "媒体任务当前状态不支持该操作：{}");
    ErrorCode MEDIA_TASK_RETRY_EXCEEDED = new ErrorCode(1_040_012_022, "媒体任务重试次数已达上限");
    ErrorCode MEDIA_SENSITIVE_DISABLED = new ErrorCode(1_040_012_023, "敏感媒体能力({})未开启");
    ErrorCode MEDIA_SENSITIVE_FORBIDDEN = new ErrorCode(1_040_012_024, "无权使用敏感媒体能力({})");
    ErrorCode MEDIA_SENSITIVE_CONFIRM_REQUIRED = new ErrorCode(1_040_012_025, "敏感媒体能力({})需要显式确认授权和风险");
    ErrorCode MEDIA_SENSITIVE_SOURCE_INVALID = new ErrorCode(1_040_012_026, "敏感媒体素材不合法：{}");
    ErrorCode MEDIA_SENSITIVE_SOURCE_FORBIDDEN = new ErrorCode(1_040_012_027, "无权使用该敏感媒体素材");

    // ========== AI 音色库 1-040-013-000 ==========
    ErrorCode VOICE_PROFILE_NOT_EXISTS = new ErrorCode(1_040_013_000, "音色不存在");
    ErrorCode VOICE_PROFILE_FORBIDDEN = new ErrorCode(1_040_013_001, "无权访问该音色");
    ErrorCode VOICE_PROFILE_CONFIG_ERROR = new ErrorCode(1_040_013_002, "音色配置错误：{}");
    ErrorCode VOICE_PROFILE_DISABLE = new ErrorCode(1_040_013_003, "音色已停用");

}
