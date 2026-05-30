package cn.iocoder.yudao.module.ai.enums.model;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * AI 模型使用场景枚举。
 *
 * @author codex
 */
@Getter
@RequiredArgsConstructor
public enum AiModelSceneEnum implements ArrayValuable<String> {

    CHAT_DEFAULT("chat.default", "默认对话"),
    IMAGE_TEXT_TO_IMAGE("image.text-to-image", "文生图"),
    IMAGE_IMAGE_TO_IMAGE("image.image-to-image", "图生图"),
    VIDEO_TEXT_TO_VIDEO("video.text-to-video", "文生视频"),
    VIDEO_IMAGE_TO_VIDEO("video.image-to-video", "图生视频"),
    VOICE_TTS("voice.tts", "文本转语音"),
    VOICE_ASR("voice.asr", "语音识别"),
    KNOWLEDGE_EMBEDDING("knowledge.embedding", "知识库向量化"),
    KNOWLEDGE_RERANK("knowledge.rerank", "知识库重排");

    /**
     * 场景编码
     */
    private final String scene;
    /**
     * 场景名
     */
    private final String name;

    public static final String[] ARRAYS = Arrays.stream(values()).map(AiModelSceneEnum::getScene)
            .toArray(String[]::new);

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
