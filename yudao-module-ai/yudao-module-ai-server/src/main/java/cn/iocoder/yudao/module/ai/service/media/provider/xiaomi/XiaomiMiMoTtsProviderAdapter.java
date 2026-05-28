package cn.iocoder.yudao.module.ai.service.media.provider.xiaomi;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapter;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderException;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderResult;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class XiaomiMiMoTtsProviderAdapter implements AiMediaProviderAdapter {

    public static final String MODEL_TTS = "mimo-v2.5-tts";
    public static final String MODEL_VOICE_CLONE = "mimo-v2.5-tts-voiceclone";
    public static final String MODEL_VOICE_DESIGN = "mimo-v2.5-tts-voicedesign";
    public static final String MODEL = MODEL_TTS;

    /**
     * 小米要求音色克隆样本 Base64 后不超过 10 MB。这里下载原文件限制略低，给 Base64 膨胀留余量。
     */
    private static final long MAX_CLONE_SAMPLE_DOWNLOAD_BYTES = 7_500_000L;
    private static final int MAX_CLONE_SAMPLE_BASE64_LENGTH = 10 * 1024 * 1024;

    private static final String DEFAULT_BASE_URL = "https://api.xiaomimimo.com";
    private static final String API_VERSION_PATH = "/v1";
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String CHAT_COMPLETIONS_SUFFIX = "/chat/completions";

    @Resource
    private AiModelService modelService;
    @Resource
    private AiApiKeyService apiKeyService;
    @Resource
    private AiRemoteResourceService remoteResourceService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(Integer taskType, String platform, String model) {
        return Objects.equals(AiMediaTaskTypeEnum.TTS.getType(), taskType)
                && Objects.equals(AiPlatformEnum.XIAOMI_MIMO.getPlatform(), platform)
                && (Objects.equals(MODEL_TTS, model)
                || Objects.equals(MODEL_VOICE_CLONE, model)
                || Objects.equals(MODEL_VOICE_DESIGN, model));
    }

    @Override
    @SneakyThrows
    public AiMediaProviderResult execute(AiMediaTaskDO task) {
        AiModelDO model = modelService.validateModel(task.getModelId());
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        if (StrUtil.isBlank(apiKey.getApiKey())) {
            throw new AiMediaProviderException("MISSING_API_KEY", "小米 MiMo API Key 未配置");
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(apiKey.getStatus())) {
            throw new AiMediaProviderException("DISABLED_API_KEY", "小米 MiMo API Key 已禁用");
        }

        Map<String, Object> request = buildRequest(task);
        String url = buildUrl(apiKey.getUrl());
        String requestBody = objectMapper.writeValueAsString(request);
        try (HttpResponse response = HttpRequest.post(url)
                .header("api-key", apiKey.getApiKey())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .timeout(120_000)
                .execute()) {
            if (response.getStatus() < HttpStatus.HTTP_OK || response.getStatus() >= HttpStatus.HTTP_MULT_CHOICE) {
                throw new AiMediaProviderException("HTTP_" + response.getStatus(),
                        StrUtil.maxLength(response.body(), 480));
            }
            return parseResponse(response.body(), task);
        } catch (AiMediaProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiMediaProviderException("NETWORK_ERROR", "调用小米 MiMo TTS 失败", e);
        }
    }

    private Map<String, Object> buildRequest(AiMediaTaskDO task) {
        Map<String, Object> input = task.getInputJson();
        String text = Objects.toString(input.get("text"), "");
        String stylePrompt = Objects.toString(input.get("stylePrompt"), "");
        String voice = Objects.toString(input.get("voice"), "");
        String format = Objects.toString(input.get("format"), "");
        AiVoiceProfileSourceTypeEnum voiceMode = parseVoiceMode(Objects.toString(input.get("voiceMode"), null));

        List<Map<String, Object>> messages = new ArrayList<>();
        if (voiceMode == AiVoiceProfileSourceTypeEnum.DESIGN) {
            messages.add(Map.of("role", "user", "content", Objects.toString(input.get("designPrompt"), "")));
        } else if (StrUtil.isNotBlank(stylePrompt)) {
            messages.add(Map.of("role", "user", "content", stylePrompt));
        }
        messages.add(Map.of("role", "assistant", "content", text));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", task.getModel());
        request.put("messages", messages);
        request.put("stream", false);
        request.put("audio", buildAudioOptions(input, voiceMode, voice, format));
        return request;
    }

    private Map<String, Object> buildAudioOptions(Map<String, Object> input, AiVoiceProfileSourceTypeEnum voiceMode,
                                                  String voice, String format) {
        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("format", format);
        if (voiceMode == AiVoiceProfileSourceTypeEnum.BUILTIN) {
            audio.put("voice", voice);
        } else if (voiceMode == AiVoiceProfileSourceTypeEnum.CLONE) {
            audio.put("voice", buildCloneVoiceDataUri(input));
        } else if (voiceMode == AiVoiceProfileSourceTypeEnum.DESIGN) {
            audio.put("optimize_text_preview", true);
        }
        return audio;
    }

    private String buildCloneVoiceDataUri(Map<String, Object> input) {
        String sampleFileUrl = Objects.toString(input.get("sampleFileUrl"), "");
        if (StrUtil.isBlank(sampleFileUrl)) {
            throw new AiMediaProviderException("MISSING_SAMPLE", "音色克隆样本不能为空");
        }
        String mimeType = normalizeSampleMimeType(Objects.toString(input.get("sampleMimeType"), ""), sampleFileUrl);
        if (StrUtil.isBlank(mimeType)) {
            throw new AiMediaProviderException("UNSUPPORTED_SAMPLE", "音色克隆样本只支持 mp3/wav");
        }
        byte[] sample = remoteResourceService.download(sampleFileUrl, MAX_CLONE_SAMPLE_DOWNLOAD_BYTES);
        String base64 = Base64.encode(sample);
        if (base64.length() > MAX_CLONE_SAMPLE_BASE64_LENGTH) {
            throw new AiMediaProviderException("SAMPLE_TOO_LARGE", "音色克隆样本 Base64 后超过 10MB");
        }
        return "data:" + mimeType + ";base64," + base64;
    }

    private String normalizeSampleMimeType(String mimeType, String sampleFileUrl) {
        if (StrUtil.equalsAnyIgnoreCase(mimeType, "audio/mpeg", "audio/mp3")) {
            return "audio/mpeg";
        }
        if (StrUtil.equalsAnyIgnoreCase(mimeType, "audio/wav", "audio/wave", "audio/x-wav")) {
            return "audio/wav";
        }
        if (StrUtil.endWithIgnoreCase(sampleFileUrl, ".mp3")) {
            return "audio/mpeg";
        }
        if (StrUtil.endWithIgnoreCase(sampleFileUrl, ".wav")) {
            return "audio/wav";
        }
        return null;
    }

    private String buildUrl(String configuredUrl) {
        String baseUrl = StrUtil.blankToDefault(configuredUrl, DEFAULT_BASE_URL);
        baseUrl = StrUtil.removeSuffix(baseUrl, "/");
        if (StrUtil.endWith(baseUrl, CHAT_COMPLETIONS_PATH) || StrUtil.endWith(baseUrl, CHAT_COMPLETIONS_SUFFIX)) {
            return baseUrl;
        }
        if (StrUtil.endWith(baseUrl, API_VERSION_PATH)) {
            return baseUrl + CHAT_COMPLETIONS_SUFFIX;
        }
        return baseUrl + CHAT_COMPLETIONS_PATH;
    }

    @SneakyThrows
    private AiMediaProviderResult parseResponse(String body, AiMediaTaskDO task) {
        JsonNode root = objectMapper.readTree(body);
        String audioBase64 = findAudioBase64(root);
        if (StrUtil.isBlank(audioBase64)) {
            log.warn("[parseResponse][小米 MiMo TTS 返回无法识别，taskId({}) body({})]", task.getId(), StrUtil.maxLength(body, 1024));
            throw new AiMediaProviderException("INVALID_RESPONSE", "小米 MiMo TTS 未返回 base64 音频");
        }
        String format = Objects.toString(task.getInputJson().get("format"), "mp3");
        byte[] audio = Base64.decode(audioBase64);
        Map<String, Object> rawOutput = sanitizeRawOutput(root);
        return new AiMediaProviderResult(audio, "tts-" + task.getId() + "." + format,
                getMimeType(format), rawOutput);
    }

    private Map<String, Object> sanitizeRawOutput(JsonNode root) {
        Map<String, Object> rawOutput = new LinkedHashMap<>();
        rawOutput.put("id", root.path("id").asText(null));
        rawOutput.put("model", root.path("model").asText(null));
        rawOutput.put("object", root.path("object").asText(null));
        rawOutput.put("created", root.path("created").asLong(0));
        rawOutput.put("finishReason", root.at("/choices/0/finish_reason").asText(null));
        rawOutput.put("usage", objectMapper.convertValue(root.path("usage"), new TypeReference<Map<String, Object>>() {}));
        return rawOutput;
    }

    private String findAudioBase64(JsonNode root) {
        JsonNode audio = root.at("/choices/0/message/audio/data");
        if (audio.isTextual()) {
            return audio.asText();
        }
        audio = root.at("/choices/0/message/audio");
        if (audio.isTextual()) {
            return audio.asText();
        }
        audio = root.at("/data/audio");
        if (audio.isTextual()) {
            return audio.asText();
        }
        audio = root.get("audio");
        return audio != null && audio.isTextual() ? audio.asText() : null;
    }

    private String getMimeType(String format) {
        return switch (StrUtil.blankToDefault(format, "mp3").toLowerCase()) {
            case "wav" -> "audio/wav";
            case "pcm" -> "audio/L16";
            default -> "audio/mpeg";
        };
    }

    private AiVoiceProfileSourceTypeEnum parseVoiceMode(String voiceMode) {
        String type = StrUtil.blankToDefault(voiceMode, AiVoiceProfileSourceTypeEnum.BUILTIN.getType());
        for (AiVoiceProfileSourceTypeEnum value : AiVoiceProfileSourceTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return AiVoiceProfileSourceTypeEnum.BUILTIN;
    }

}
