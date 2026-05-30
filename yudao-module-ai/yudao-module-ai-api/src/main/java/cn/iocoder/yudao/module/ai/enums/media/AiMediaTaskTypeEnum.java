package cn.iocoder.yudao.module.ai.enums.media;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * AI 媒体任务类型枚举。
 *
 * @author codex
 */
@Getter
@RequiredArgsConstructor
public enum AiMediaTaskTypeEnum implements ArrayValuable<Integer> {

    UNKNOWN(0, "未知"),
    TTS(1, "文本转语音"),
    IMAGE_TO_VIDEO(2, "图生视频"),
    TEXT_TO_VIDEO(3, "文生视频");

    private final Integer type;
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(AiMediaTaskTypeEnum::getType)
            .toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
