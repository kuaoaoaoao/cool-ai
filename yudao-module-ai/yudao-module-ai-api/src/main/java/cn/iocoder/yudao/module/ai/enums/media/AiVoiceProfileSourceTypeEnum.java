package cn.iocoder.yudao.module.ai.enums.media;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * AI 音色来源类型枚举。
 *
 * @author codex
 */
@Getter
@RequiredArgsConstructor
public enum AiVoiceProfileSourceTypeEnum implements ArrayValuable<String> {

    BUILTIN("BUILTIN", "预置音色"),
    CLONE("CLONE", "音色克隆"),
    DESIGN("DESIGN", "文字设计音色");

    private final String type;
    private final String name;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(AiVoiceProfileSourceTypeEnum::getType)
            .toArray(String[]::new);

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
