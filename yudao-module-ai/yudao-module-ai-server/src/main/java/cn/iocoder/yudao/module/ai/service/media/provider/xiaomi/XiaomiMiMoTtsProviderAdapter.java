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
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
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

    public static final String MODEL = "mimo-v2.5-tts";

    private static final String DEFAULT_BASE_URL = "https://api.xiaomimimo.com";
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    @Resource
    private AiModelService modelService;
    @Resource
    private AiApiKeyService apiKeyService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(Integer taskType, String platform, String model) {
        return Objects.equals(AiMediaTaskTypeEnum.TTS.getType(), taskType)
                && Objects.equals(AiPlatformEnum.XIAOMI_MIMO.getPlatform(), platform)
                && Objects.equals(MODEL, model);
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
                        StrUtil.maxLength(response.body(), 512));
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

        List<Map<String, Object>> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(stylePrompt)) {
            messages.add(Map.of("role", "user", "content", stylePrompt));
        }
        messages.add(Map.of("role", "assistant", "content", text));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", MODEL);
        request.put("messages", messages);
        request.put("stream", false);
        request.put("audio", Map.of("voice", voice, "format", format));
        return request;
    }

    private String buildUrl(String configuredUrl) {
        String baseUrl = StrUtil.blankToDefault(configuredUrl, DEFAULT_BASE_URL);
        return StrUtil.removeSuffix(baseUrl, "/") + CHAT_COMPLETIONS_PATH;
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
        Map<String, Object> rawOutput = objectMapper.convertValue(root, new TypeReference<>() {});
        return new AiMediaProviderResult(audio, "tts-" + task.getId() + "." + format,
                getMimeType(format), rawOutput);
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

}
