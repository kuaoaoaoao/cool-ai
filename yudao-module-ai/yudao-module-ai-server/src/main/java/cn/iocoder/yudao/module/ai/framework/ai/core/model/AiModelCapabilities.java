package cn.iocoder.yudao.module.ai.framework.ai.core.model;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.media.provider.xiaomi.XiaomiMiMoTtsProviderAdapter;
import cn.iocoder.yudao.module.ai.util.AiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 模型能力推断工具。
 *
 * 第一阶段先通过平台、模型类型、模型 ID 推断能力，不改 ai_model 表结构。
 *
 * @author kuao
 */
public class AiModelCapabilities {

    public static final String CAPABILITY_CHAT = "chat";
    public static final String CAPABILITY_IMAGE = "image";
    public static final String CAPABILITY_VISION = "vision";
    public static final String CAPABILITY_AUDIO = "audio";
    public static final String CAPABILITY_VIDEO = "video";
    public static final String CAPABILITY_STREAM = "stream";
    public static final String CAPABILITY_TOOL = "tool";
    public static final String CAPABILITY_EMBEDDING = "embedding";
    public static final String CAPABILITY_TTS = "tts";
    public static final String CAPABILITY_OPENAI_COMPATIBLE = "openai-compatible";
    public static final String CAPABILITY_ANTHROPIC_COMPATIBLE = "anthropic-compatible";

    public static final String MODALITY_TEXT = "text";
    public static final String MODALITY_IMAGE = "image";
    public static final String MODALITY_AUDIO = "audio";
    public static final String MODALITY_VIDEO = "video";

    public static AiModelCapability infer(AiModelDO model) {
        AiModelCapability capability = new AiModelCapability()
                .setCapabilities(new ArrayList<>())
                .setInputModalities(new ArrayList<>())
                .setOutputModalities(new ArrayList<>())
                .setConfigTips(new ArrayList<>());
        if (model == null) {
            return capability;
        }
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        Integer type = model.getType();
        if (AiModelTypeEnum.CHAT.getType().equals(type)) {
            inferChat(capability, platform, model.getModel());
        } else if (AiModelTypeEnum.IMAGE.getType().equals(type)) {
            add(capability.getCapabilities(), CAPABILITY_IMAGE);
            add(capability.getInputModalities(), MODALITY_TEXT);
            add(capability.getOutputModalities(), MODALITY_IMAGE);
        } else if (AiModelTypeEnum.EMBEDDING.getType().equals(type)) {
            add(capability.getCapabilities(), CAPABILITY_EMBEDDING);
            add(capability.getInputModalities(), MODALITY_TEXT);
            add(capability.getOutputModalities(), "vector");
        } else if (AiModelTypeEnum.VOICE.getType().equals(type)) {
            inferVoice(capability, platform, model.getModel());
        }
        appendConfigTips(capability, platform, type, model.getModel());
        return capability;
    }

    private static void inferChat(AiModelCapability capability, AiPlatformEnum platform, String model) {
        add(capability.getCapabilities(), CAPABILITY_CHAT, CAPABILITY_STREAM);
        add(capability.getInputModalities(), MODALITY_TEXT);
        add(capability.getOutputModalities(), MODALITY_TEXT);
        if (AiUtils.supportImageInput(platform, model)) {
            add(capability.getCapabilities(), CAPABILITY_VISION);
            add(capability.getInputModalities(), MODALITY_IMAGE);
        }
        if (AiUtils.supportAudioInput(platform, model)) {
            add(capability.getCapabilities(), CAPABILITY_AUDIO);
            add(capability.getInputModalities(), MODALITY_AUDIO);
        }
        if (AiUtils.supportVideoInput(platform, model)) {
            add(capability.getCapabilities(), CAPABILITY_VIDEO);
            add(capability.getInputModalities(), MODALITY_VIDEO);
        }
        if (supportsTool(platform)) {
            add(capability.getCapabilities(), CAPABILITY_TOOL);
        }
        if (platform == AiPlatformEnum.OPENAI || platform == AiPlatformEnum.GEMINI
                || platform == AiPlatformEnum.BAI_CHUAN || platform == AiPlatformEnum.XIAOMI_MIMO
                || platform == AiPlatformEnum.GROK) {
            add(capability.getCapabilities(), CAPABILITY_OPENAI_COMPATIBLE);
        }
        if (platform == AiPlatformEnum.ANTHROPIC) {
            add(capability.getCapabilities(), CAPABILITY_ANTHROPIC_COMPATIBLE);
        }
    }

    private static void inferVoice(AiModelCapability capability, AiPlatformEnum platform, String model) {
        if (platform == AiPlatformEnum.XIAOMI_MIMO
                && (XiaomiMiMoTtsProviderAdapter.MODEL_TTS.equals(model)
                || XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_CLONE.equals(model)
                || XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_DESIGN.equals(model))) {
            add(capability.getCapabilities(), CAPABILITY_TTS);
            add(capability.getInputModalities(), MODALITY_TEXT);
            add(capability.getOutputModalities(), MODALITY_AUDIO);
        }
    }

    private static boolean supportsTool(AiPlatformEnum platform) {
        return switch (platform) {
            case TONG_YI, DEEP_SEEK, DOU_BAO, HUN_YUAN, SILICON_FLOW, ZHI_PU, MINI_MAX, MOONSHOT,
                    XING_HUO, BAI_CHUAN, XIAOMI_MIMO, OPENAI, AZURE_OPENAI, ANTHROPIC, GEMINI, GROK -> true;
            default -> false;
        };
    }

    private static void appendConfigTips(AiModelCapability capability, AiPlatformEnum platform, Integer type,
            String model) {
        List<String> tips = capability.getConfigTips();
        if (platform == AiPlatformEnum.XIAOMI_MIMO) {
            tips.add("API URL 建议填写 https://api.xiaomimimo.com/v1");
            if (AiModelTypeEnum.CHAT.getType().equals(type)) {
                tips.add("图文多模态优先使用 mimo-v2.5；纯文本高质量优先使用 mimo-v2.5-pro；低成本优先使用 mimo-v2-flash");
            }
            if (AiModelTypeEnum.VOICE.getType().equals(type)) {
                tips.add("语音模型可配置 mimo-v2.5-tts、mimo-v2.5-tts-voiceclone、mimo-v2.5-tts-voicedesign");
            }
        } else if (platform == AiPlatformEnum.OPENAI) {
            tips.add("可接 OpenAI 官方，也可接 OpenAI-compatible 服务商；兼容服务商需要填写自定义 API URL");
        } else if (platform == AiPlatformEnum.ANTHROPIC) {
            tips.add("Anthropic 原生协议适合 Claude；第三方兼容服务需要确认是否支持 Messages API");
        } else if (platform == AiPlatformEnum.OLLAMA) {
            tips.add("Ollama 需要填写本地或内网服务 URL，例如 http://127.0.0.1:11434");
        }
        if (StrUtil.containsIgnoreCase(model, "dall-e")) {
            tips.add("OpenAI DALL-E 属于旧图像模型，后续建议评估 GPT Image 兼容性");
        }
    }

    private static void add(List<String> values, String... items) {
        for (String item : items) {
            if (!values.contains(item)) {
                values.add(item);
            }
        }
    }

}
