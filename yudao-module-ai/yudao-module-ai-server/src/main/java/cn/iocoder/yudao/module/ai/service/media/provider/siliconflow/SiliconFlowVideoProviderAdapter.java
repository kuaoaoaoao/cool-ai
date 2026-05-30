package cn.iocoder.yudao.module.ai.service.media.provider.siliconflow;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapter;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderException;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderQueryResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderSubmitResult;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class SiliconFlowVideoProviderAdapter implements AiMediaProviderAdapter {

    public static final String MODEL_IMAGE_TO_VIDEO = "Wan-AI/Wan2.2-I2V-A14B";

    private static final String DEFAULT_BASE_URL = "https://api.siliconflow.cn";
    private static final String API_VERSION_PATH = "/v1";
    private static final String SUBMIT_PATH = "/v1/video/submit";
    private static final String SUBMIT_SUFFIX = "/video/submit";
    private static final String STATUS_PATH = "/v1/video/status";
    private static final String STATUS_SUFFIX = "/video/status";

    @Resource
    private AiModelService modelService;
    @Resource
    private AiApiKeyService apiKeyService;
    @Resource
    private AiRemoteResourceService remoteResourceService;
    @Resource
    private ObjectMapper objectMapper;

    @Value("${yudao.ai.media.video.max-source-bytes:10485760}")
    private Long maxSourceBytes;
    @Value("${yudao.ai.media.video.request-timeout-millis:120000}")
    private Integer requestTimeoutMillis;

    @Override
    public boolean supports(Integer taskType, String platform, String model) {
        return Objects.equals(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(), taskType)
                && Objects.equals(AiPlatformEnum.SILICON_FLOW.getPlatform(), platform)
                && StrUtil.isNotBlank(model);
    }

    @Override
    public AiMediaProviderResult execute(AiMediaTaskDO task) {
        throw new UnsupportedOperationException("SiliconFlow video uses async submit/query");
    }

    @Override
    @SneakyThrows
    public AiMediaProviderSubmitResult submit(AiMediaTaskDO task) {
        AiApiKeyDO apiKey = validateApiKey(task);
        Map<String, Object> request = buildSubmitRequest(task);
        String requestBody = objectMapper.writeValueAsString(request);
        String body = doPost(buildUrl(apiKey.getUrl(), SUBMIT_PATH, SUBMIT_SUFFIX), apiKey.getApiKey(), requestBody);
        JsonNode root = objectMapper.readTree(body);
        String requestId = firstText(root, "requestId", "request_id", "id", "task_id");
        if (StrUtil.isBlank(requestId)) {
            log.warn("[submit][SiliconFlow 视频提交响应无法识别，taskId({}) body({})]", task.getId(), StrUtil.maxLength(body, 1024));
            throw new AiMediaProviderException("INVALID_RESPONSE", "SiliconFlow 未返回 requestId");
        }
        Map<String, Object> rawOutput = objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
        return new AiMediaProviderSubmitResult(requestId, AiMediaTaskStatusEnum.QUEUED.getStatus(),
                5, "submitted", rawOutput);
    }

    @Override
    @SneakyThrows
    public AiMediaProviderQueryResult query(AiMediaTaskDO task) {
        if (StrUtil.isBlank(task.getProviderTaskId())) {
            throw new AiMediaProviderException("MISSING_REQUEST_ID", "SiliconFlow requestId 不能为空");
        }
        AiApiKeyDO apiKey = validateApiKey(task);
        String requestBody = objectMapper.writeValueAsString(Map.of("requestId", task.getProviderTaskId()));
        String body = doPost(buildUrl(apiKey.getUrl(), STATUS_PATH, STATUS_SUFFIX), apiKey.getApiKey(), requestBody);
        JsonNode root = objectMapper.readTree(body);
        String providerStatus = firstText(root, "status", "taskStatus", "task_status");
        Map<String, Object> rawOutput = objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
        return mapStatus(providerStatus, root, rawOutput);
    }

    @Override
    public void cancel(AiMediaTaskDO task) {
        throw new UnsupportedOperationException("SiliconFlow video cancel is not supported yet");
    }

    private Map<String, Object> buildSubmitRequest(AiMediaTaskDO task) {
        Map<String, Object> input = task.getInputJson();
        String prompt = Objects.toString(input.get("prompt"), "");
        String image = Objects.toString(input.get("sourceImageUrl"), "");
        String imageSize = Objects.toString(input.get("imageSize"), "1280x720");
        if (StrUtil.isBlank(prompt)) {
            throw new AiMediaProviderException("MISSING_PROMPT", "SiliconFlow 图生视频提示词不能为空");
        }
        if (StrUtil.isBlank(image)) {
            throw new AiMediaProviderException("MISSING_IMAGE", "SiliconFlow 图生视频源图片不能为空");
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", task.getModel());
        request.put("prompt", prompt);
        request.put("image_size", imageSize);
        request.put("image", buildImagePayload(task, image, input));
        putIfPresent(request, "negative_prompt", input.get("negativePrompt"));
        putIfPresent(request, "seed", input.get("seed"));
        return request;
    }

    private String buildImagePayload(AiMediaTaskDO task, String imageUrl, Map<String, Object> input) {
        if (!remoteResourceService.isPrivateUrl(imageUrl)) {
            return imageUrl;
        }
        if (!isProjectFileUrl(imageUrl)) {
            throw new AiMediaProviderException("PRIVATE_IMAGE_URL_UNSUPPORTED",
                    "本地或内网图片仅支持项目文件服务 URL，请使用上传组件返回的文件地址或公网图片 URL");
        }
        try {
            long limit = maxSourceBytes == null || maxSourceBytes <= 0 ? 10_485_760L : maxSourceBytes;
            byte[] image = remoteResourceService.downloadPrivateUrl(imageUrl, limit);
            String mimeType = normalizeImageMimeType(Objects.toString(input.get("sourceMimeType"), ""), imageUrl);
            String base64 = Base64.getEncoder().encodeToString(image);
            log.info("[buildImagePayload][taskId({}) 检测到本地/内网图片 URL，已转换为 base64 提交]", task.getId());
            return "data:" + mimeType + ";base64," + base64;
        } catch (AiMediaProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiMediaProviderException("IMAGE_BASE64_FALLBACK_FAIL",
                    "本地图片转 base64 失败：" + e.getMessage(), e);
        }
    }

    private boolean isProjectFileUrl(String imageUrl) {
        try {
            String path = URI.create(imageUrl.trim()).getPath();
            return StrUtil.contains(path, "/infra/file/") && StrUtil.contains(path, "/get/");
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeImageMimeType(String sourceMimeType, String imageUrl) {
        if (StrUtil.startWithIgnoreCase(sourceMimeType, "image/")) {
            return sourceMimeType.toLowerCase();
        }
        String lowerUrl = StrUtil.nullToEmpty(imageUrl).toLowerCase();
        if (StrUtil.containsAny(lowerUrl, ".jpg", ".jpeg")) {
            return "image/jpeg";
        }
        if (StrUtil.contains(lowerUrl, ".webp")) {
            return "image/webp";
        }
        return "image/png";
    }

    private AiApiKeyDO validateApiKey(AiMediaTaskDO task) {
        AiModelDO model = modelService.validateModel(task.getModelId());
        if (!Objects.equals(AiPlatformEnum.SILICON_FLOW.getPlatform(), model.getPlatform())) {
            throw new AiMediaProviderException("MODEL_PLATFORM_MISMATCH", "SiliconFlow 视频模型平台不匹配");
        }
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        if (StrUtil.isBlank(apiKey.getApiKey())) {
            throw new AiMediaProviderException("MISSING_API_KEY", "SiliconFlow API Key 未配置");
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(apiKey.getStatus())) {
            throw new AiMediaProviderException("DISABLED_API_KEY", "SiliconFlow API Key 已禁用");
        }
        return apiKey;
    }

    private String doPost(String url, String apiKey, String requestBody) {
        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .timeout(requestTimeoutMillis == null || requestTimeoutMillis <= 0 ? 120_000 : requestTimeoutMillis)
                .execute()) {
            if (response.getStatus() < HttpStatus.HTTP_OK || response.getStatus() >= HttpStatus.HTTP_MULT_CHOICE) {
                throw new AiMediaProviderException("HTTP_" + response.getStatus(),
                        StrUtil.maxLength(response.body(), 480));
            }
            return response.body();
        } catch (AiMediaProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiMediaProviderException("NETWORK_ERROR", "调用 SiliconFlow 视频接口失败", e);
        }
    }

    private AiMediaProviderQueryResult mapStatus(String providerStatus, JsonNode root, Map<String, Object> rawOutput) {
        if (StrUtil.equalsIgnoreCase(providerStatus, "Succeed")) {
            String resultUrl = firstText(root.at("/results/videos/0"), "url");
            if (StrUtil.isBlank(resultUrl)) {
                resultUrl = firstText(root, "url", "videoUrl", "video_url");
            }
            if (StrUtil.isBlank(resultUrl)) {
                throw new AiMediaProviderException("INVALID_RESPONSE", "SiliconFlow 成功响应未返回视频 URL");
            }
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), 100,
                    "provider_succeeded", resultUrl, "video/mp4", null, null, rawOutput);
        }
        if (StrUtil.equalsIgnoreCase(providerStatus, "Failed")) {
            String errorMessage = firstText(root, "error", "message", "reason");
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.FAILED.getStatus(), 100,
                    "provider_failed", null, null, "PROVIDER_FAILED",
                    StrUtil.blankToDefault(errorMessage, "SiliconFlow 视频生成失败"), rawOutput);
        }
        if (StrUtil.equalsIgnoreCase(providerStatus, "InProgress")) {
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.RUNNING.getStatus(), 50,
                    "provider_running", null, null, null, null, rawOutput);
        }
        if (StrUtil.equalsIgnoreCase(providerStatus, "InQueue")) {
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.QUEUED.getStatus(), 10,
                    "provider_queueing", null, null, null, null, rawOutput);
        }
        throw new AiMediaProviderException("UNKNOWN_STATUS",
                StrUtil.format("SiliconFlow 视频任务状态无法识别：{}", providerStatus));
    }

    private String buildUrl(String configuredUrl, String path, String suffix) {
        String baseUrl = StrUtil.blankToDefault(configuredUrl, DEFAULT_BASE_URL);
        baseUrl = StrUtil.removeSuffix(baseUrl, "/");
        if (StrUtil.endWith(baseUrl, path) || StrUtil.endWith(baseUrl, suffix)) {
            return baseUrl;
        }
        if (StrUtil.endWith(baseUrl, API_VERSION_PATH)) {
            return baseUrl + suffix;
        }
        return baseUrl + path;
    }

    private void putIfPresent(Map<String, Object> request, String key, Object value) {
        if (value == null || StrUtil.isBlank(Objects.toString(value, ""))) {
            return;
        }
        request.put(key, value);
    }

    private String firstText(JsonNode root, String... fieldNames) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode node = root.get(fieldName);
            if (node != null && node.isValueNode() && StrUtil.isNotBlank(node.asText())) {
                return node.asText();
            }
        }
        return null;
    }

}
