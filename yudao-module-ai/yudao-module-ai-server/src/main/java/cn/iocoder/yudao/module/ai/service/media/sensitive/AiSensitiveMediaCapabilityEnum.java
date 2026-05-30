package cn.iocoder.yudao.module.ai.service.media.sensitive;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum AiSensitiveMediaCapabilityEnum {

    PERSON_REPLACEMENT("PERSON_REPLACEMENT", "人物替换", "ai:media:person-replacement:create",
            Set.of("video/mp4", "video/quicktime", "image/jpeg", "image/jpg", "image/png", "image/webp")),
    LOCAL_VOICE_CLONE("LOCAL_VOICE_CLONE", "本地声音克隆", "ai:media:voice-clone:create",
            Set.of("audio/mpeg", "audio/mp3", "audio/wav", "audio/wave", "audio/x-wav")),
    LOCAL_VOICE_CONVERSION("LOCAL_VOICE_CONVERSION", "本地语音转换", "ai:media:voice-conversion:create",
            Set.of("audio/mpeg", "audio/mp3", "audio/wav", "audio/wave", "audio/x-wav"));

    private final String capability;
    private final String name;
    private final String permission;
    private final Set<String> supportedMimeTypes;

    public static AiSensitiveMediaCapabilityEnum of(String capability) {
        for (AiSensitiveMediaCapabilityEnum value : values()) {
            if (value.capability.equals(capability)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知敏感媒体能力：" + capability);
    }

}
