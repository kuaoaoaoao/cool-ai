package cn.iocoder.yudao.module.ai.framework.ai.core.model.xiaomi;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import cn.iocoder.yudao.module.ai.util.FileTypeUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.CHAT_MODEL_NOT_SUPPORT_AUDIO;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.CHAT_MODEL_NOT_SUPPORT_VIDEO;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.CHAT_MULTIMODAL_ATTACHMENT_FAIL;

/**
 * 小米 MiMo 全模态 Chat Completions 客户端。
 *
 * <p>Spring AI OpenAI 适配器可以覆盖文本/图片，但暂时无法稳定表达 MiMo 的
 * {@code input_audio} 和 {@code video_url + fps + media_resolution} 结构，所以音频/视频走这里。</p>
 */
@Component
@Slf4j
public class XiaomiMiMoMultimodalChatClient {

    public static final String CURRENT_USER_MESSAGE_METADATA_KEY = "yudao_current_user_message";

    private static final String API_VERSION_PATH = "/v1";
    private static final String CHAT_COMPLETIONS_SUFFIX = "/chat/completions";
    private static final int MAX_BASE64_LENGTH = 50 * 1024 * 1024;

    @Value("${yudao.ai.xiaomi-mimo.multimodal.max-inline-bytes:37500000}")
    private Long maxInlineBytes;
    @Value("${yudao.ai.xiaomi-mimo.multimodal.video.fps:2}")
    private Integer videoFps;
    @Value("${yudao.ai.xiaomi-mimo.multimodal.video.media-resolution:default}")
    private String videoMediaResolution;

    @Resource
    private AiApiKeyService apiKeyService;
    @Resource
    private AiRemoteResourceService remoteResourceService;
    @Resource
    private ObjectMapper objectMapper;

    public boolean supports(AiModelDO model, List<String> attachmentUrls) {
        if (model == null || !Objects.equals(AiPlatformEnum.XIAOMI_MIMO.getPlatform(), model.getPlatform())) {
            return false;
        }
        return hasAudioAttachment(attachmentUrls) || hasVideoAttachment(attachmentUrls);
    }

    public XiaomiMiMoChatResult call(AiModelDO model, Prompt prompt, List<String> attachmentUrls) {
        validateSupport(model, attachmentUrls);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        Map<String, Object> request = buildRequest(model, prompt, attachmentUrls);
        String requestBody = writeJson(request);
        try (HttpResponse response = HttpRequest.post(buildUrl(apiKey.getUrl()))
                .header("api-key", apiKey.getApiKey())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .timeout(180_000)
                .execute()) {
            if (response.getStatus() < HttpStatus.HTTP_OK || response.getStatus() >= HttpStatus.HTTP_MULT_CHOICE) {
                throw exception(CHAT_MULTIMODAL_ATTACHMENT_FAIL,
                        "小米 MiMo 响应异常 HTTP " + response.getStatus() + "：" + StrUtil.maxLength(response.body(), 480));
            }
            return parseResponse(response.body());
        } catch (Exception e) {
            log.warn("[call][小米 MiMo 多模态调用失败，model({})]", model.getModel(), e);
            if (e instanceof cn.iocoder.yudao.framework.common.exception.ServiceException serviceException) {
                throw serviceException;
            }
            throw exception(CHAT_MULTIMODAL_ATTACHMENT_FAIL, e.getMessage());
        }
    }

    Map<String, Object> buildRequest(AiModelDO model, Prompt prompt, List<String> attachmentUrls) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model.getModel());
        request.put("messages", buildMessages(prompt, attachmentUrls));
        request.put("stream", false);
        ChatOptions options = prompt.getOptions();
        if (options != null) {
            if (options.getTemperature() != null) {
                request.put("temperature", options.getTemperature());
            }
            if (options.getMaxTokens() != null) {
                request.put("max_completion_tokens", options.getMaxTokens());
            }
        }
        return request;
    }

    private List<Map<String, Object>> buildMessages(Prompt prompt, List<String> attachmentUrls) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Message message : prompt.getInstructions()) {
            String text = StrUtil.nullToDefault(message.getText(), "");
            List<Map<String, Object>> parts = new ArrayList<>();
            if (StrUtil.isNotBlank(text)) {
                parts.add(Map.of("type", "text", "text", text));
            }
            if (message instanceof UserMessage userMessage) {
                addImageParts(parts, userMessage.getMedia());
                if (Boolean.TRUE.equals(userMessage.getMetadata().get(CURRENT_USER_MESSAGE_METADATA_KEY))) {
                    addAudioVideoParts(parts, attachmentUrls);
                }
            }
            Map<String, Object> messageMap = new LinkedHashMap<>();
            messageMap.put("role", toRole(message.getMessageType()));
            messageMap.put("content", parts.size() == 1 && CollUtil.isEmpty(getMediaParts(parts)) ? text : parts);
            messages.add(messageMap);
        }
        return messages;
    }

    private List<Map<String, Object>> getMediaParts(List<Map<String, Object>> parts) {
        return parts.stream()
                .filter(part -> !"text".equals(part.get("type")))
                .toList();
    }

    private void addImageParts(List<Map<String, Object>> parts, List<Media> medias) {
        if (CollUtil.isEmpty(medias)) {
            return;
        }
        for (Media media : medias) {
            if (!StrUtil.startWith(media.getMimeType().toString(), "image/")) {
                continue;
            }
            parts.add(Map.of("type", "image_url",
                    "image_url", Map.of("url", toDataUri(media.getMimeType().toString(), media.getData()))));
        }
    }

    private void addAudioVideoParts(List<Map<String, Object>> parts, List<String> attachmentUrls) {
        if (CollUtil.isEmpty(attachmentUrls)) {
            return;
        }
        for (String attachmentUrl : attachmentUrls) {
            String name = getFileName(attachmentUrl);
            String mimeType = FileTypeUtils.getMineType(name);
            if (FileTypeUtils.isAudio(mimeType)) {
                parts.add(Map.of("type", "input_audio",
                        "input_audio", Map.of("data", buildAttachmentDataUri(attachmentUrl, normalizeAudioMimeType(mimeType, name)))));
            } else if (FileTypeUtils.isVideo(mimeType)) {
                Map<String, Object> part = new LinkedHashMap<>();
                part.put("type", "video_url");
                part.put("video_url", Map.of("url", buildAttachmentDataUri(attachmentUrl, normalizeVideoMimeType(mimeType, name))));
                part.put("fps", videoFps);
                part.put("media_resolution", videoMediaResolution);
                parts.add(part);
            }
        }
    }

    private String buildAttachmentDataUri(String attachmentUrl, String mimeType) {
        try {
            byte[] bytes = remoteResourceService.download(attachmentUrl, maxInlineBytes);
            String base64 = Base64.encode(bytes);
            if (base64.length() > MAX_BASE64_LENGTH) {
                throw exception(CHAT_MULTIMODAL_ATTACHMENT_FAIL, "附件 Base64 后超过 50MB");
            }
            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            log.warn("[buildAttachmentDataUri][附件({}) 处理失败]", attachmentUrl, e);
            if (e instanceof cn.iocoder.yudao.framework.common.exception.ServiceException serviceException) {
                throw serviceException;
            }
            throw exception(CHAT_MULTIMODAL_ATTACHMENT_FAIL, e.getMessage());
        }
    }

    private void validateSupport(AiModelDO model, List<String> attachmentUrls) {
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        if (hasAudioAttachment(attachmentUrls) && !AiUtils.supportAudioInput(platform, model.getModel())) {
            throw exception(CHAT_MODEL_NOT_SUPPORT_AUDIO);
        }
        if (hasVideoAttachment(attachmentUrls) && !AiUtils.supportVideoInput(platform, model.getModel())) {
            throw exception(CHAT_MODEL_NOT_SUPPORT_VIDEO);
        }
    }

    private boolean hasAudioAttachment(List<String> attachmentUrls) {
        return CollUtil.isNotEmpty(attachmentUrls) && attachmentUrls.stream()
                .map(this::getFileName)
                .map(FileTypeUtils::getMineType)
                .anyMatch(FileTypeUtils::isAudio);
    }

    private boolean hasVideoAttachment(List<String> attachmentUrls) {
        return CollUtil.isNotEmpty(attachmentUrls) && attachmentUrls.stream()
                .map(this::getFileName)
                .map(FileTypeUtils::getMineType)
                .anyMatch(FileTypeUtils::isVideo);
    }

    private String normalizeAudioMimeType(String mimeType, String name) {
        if (StrUtil.equalsAnyIgnoreCase(mimeType, "audio/mp3")) {
            return "audio/mpeg";
        }
        if (StrUtil.equalsAnyIgnoreCase(mimeType, "audio/x-wav", "audio/wave")) {
            return "audio/wav";
        }
        if (StrUtil.endWithIgnoreCase(name, ".m4a")) {
            return "audio/mp4";
        }
        return mimeType;
    }

    private String normalizeVideoMimeType(String mimeType, String name) {
        if (StrUtil.endWithIgnoreCase(name, ".mov")) {
            return "video/quicktime";
        }
        if (StrUtil.endWithIgnoreCase(name, ".avi")) {
            return "video/x-msvideo";
        }
        if (StrUtil.endWithIgnoreCase(name, ".wmv")) {
            return "video/x-ms-wmv";
        }
        return mimeType;
    }

    private String toDataUri(String mimeType, Object data) {
        if (data instanceof byte[] bytes) {
            return "data:" + mimeType + ";base64," + Base64.encode(bytes);
        }
        return Objects.toString(data, "");
    }

    private String toRole(MessageType type) {
        if (type == MessageType.SYSTEM) {
            return "system";
        }
        if (type == MessageType.ASSISTANT) {
            return "assistant";
        }
        return "user";
    }

    private String buildUrl(String configuredUrl) {
        String baseUrl = StrUtil.blankToDefault(configuredUrl, XiaomiMiMoChatModel.BASE_URL);
        baseUrl = StrUtil.removeSuffix(baseUrl, "/");
        if (StrUtil.endWith(baseUrl, CHAT_COMPLETIONS_SUFFIX)) {
            return baseUrl;
        }
        if (StrUtil.endWith(baseUrl, API_VERSION_PATH)) {
            return baseUrl + CHAT_COMPLETIONS_SUFFIX;
        }
        return baseUrl + API_VERSION_PATH + CHAT_COMPLETIONS_SUFFIX;
    }

    @SneakyThrows
    private String writeJson(Map<String, Object> request) {
        return objectMapper.writeValueAsString(request);
    }

    @SneakyThrows
    private XiaomiMiMoChatResult parseResponse(String body) {
        JsonNode root = objectMapper.readTree(body);
        JsonNode message = root.at("/choices/0/message");
        String content = parseContent(message.path("content"));
        String reasoningContent = message.path("reasoning_content").asText(null);
        if (StrUtil.isBlank(content) && StrUtil.isBlank(reasoningContent)) {
            log.warn("[parseResponse][小米 MiMo 多模态返回无法识别，body({})]", StrUtil.maxLength(body, 1024));
            throw exception(CHAT_MULTIMODAL_ATTACHMENT_FAIL, "小米 MiMo 未返回文本内容");
        }
        return new XiaomiMiMoChatResult().setContent(content).setReasoningContent(reasoningContent);
    }

    private String parseContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            List<String> texts = new ArrayList<>();
            contentNode.forEach(item -> {
                JsonNode text = item.path("text");
                if (text.isTextual()) {
                    texts.add(text.asText());
                }
            });
            return String.join("", texts);
        }
        return contentNode.toString();
    }

    private String getFileName(String url) {
        String name = FileNameUtil.getName(url);
        return StrUtil.subBefore(StrUtil.subBefore(name, "?", false), "#", false);
    }

    @Data
    public static class XiaomiMiMoChatResult {

        private String content;
        private String reasoningContent;

    }

}
