package cn.iocoder.yudao.module.ai.service.media.provider.worker;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Python Worker 媒体执行适配器。第一版用于 mock/轻量 Worker 联调。
 */
@Component
public class PythonWorkerMediaProviderAdapter implements AiMediaProviderAdapter {

    private static final String HEALTH_PATH = "/health";
    private static final String TASKS_PATH = "/tasks";

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private AiModelService modelService;
    @Resource
    private AiApiKeyService apiKeyService;

    @Value("${yudao.ai.media.worker.enable:false}")
    private Boolean enable;
    @Value("${yudao.ai.media.worker.base-url:}")
    private String baseUrl;
    @Value("${yudao.ai.media.worker.auth-token:}")
    private String authToken;
    @Value("${yudao.ai.media.worker.callback-url:}")
    private String callbackUrl;
    @Value("${yudao.ai.media.worker.request-timeout-millis:30000}")
    private Integer requestTimeoutMillis;

    @Override
    public boolean supports(Integer taskType, String platform, String model) {
        return Objects.equals(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(), taskType)
                && Objects.equals(AiPlatformEnum.PYTHON_WORKER.getPlatform(), platform)
                && StrUtil.isNotBlank(model);
    }

    @Override
    public AiMediaProviderResult execute(AiMediaTaskDO task) {
        throw new UnsupportedOperationException("Python Worker uses async submit/query");
    }

    @Override
    @SneakyThrows
    public AiMediaProviderSubmitResult submit(AiMediaTaskDO task) {
        WorkerSettings settings = resolveSettings(task);
        String body = doPost(settings, TASKS_PATH, objectMapper.writeValueAsString(buildSubmitRequest(task)));
        JsonNode root = objectMapper.readTree(body);
        if (!root.path("accepted").asBoolean(false)) {
            throw new AiMediaProviderException("WORKER_REJECTED",
                    StrUtil.blankToDefault(firstText(root, "message", "error"), "Python Worker 拒绝任务"));
        }
        String workerTaskId = firstText(root, "workerTaskId", "worker_task_id", "taskId", "id");
        if (StrUtil.isBlank(workerTaskId)) {
            throw new AiMediaProviderException("INVALID_RESPONSE", "Python Worker 未返回 workerTaskId");
        }
        return new AiMediaProviderSubmitResult(workerTaskId, AiMediaTaskStatusEnum.SUBMITTED.getStatus(),
                5, "worker_submitted", toMap(root));
    }

    @Override
    @SneakyThrows
    public AiMediaProviderQueryResult query(AiMediaTaskDO task) {
        WorkerSettings settings = resolveSettings(task);
        if (StrUtil.isBlank(task.getProviderTaskId())) {
            throw new AiMediaProviderException("MISSING_WORKER_TASK_ID", "Python Worker taskId 不能为空");
        }
        return mapStatus(objectMapper.readTree(doGet(settings, TASKS_PATH + "/" + task.getProviderTaskId())));
    }

    @Override
    public void cancel(AiMediaTaskDO task) {
        WorkerSettings settings = resolveSettings(task);
        if (StrUtil.isBlank(task.getProviderTaskId())) {
            throw new AiMediaProviderException("MISSING_WORKER_TASK_ID", "Python Worker taskId 不能为空");
        }
        doPost(settings, TASKS_PATH + "/" + task.getProviderTaskId() + "/cancel", "{}");
    }

    @Override
    public AiMediaProviderQueryResult handleCallback(AiMediaTaskDO task, Object payload) {
        return mapStatus(objectMapper.valueToTree(payload));
    }

    @Override
    public boolean verifyCallbackToken(String token) {
        return StrUtil.isNotBlank(authToken) && Objects.equals(authToken, token);
    }

    @SneakyThrows
    public boolean healthCheck() {
        return Boolean.TRUE.equals(getHealthStatus().get("ok"));
    }

    @SneakyThrows
    public Map<String, Object> getHealthStatus() {
        WorkerSettings settings = resolveConfiguredSettings();
        JsonNode root = objectMapper.readTree(doGet(settings, HEALTH_PATH));
        boolean ok = root.path("ok").asBoolean(false)
                || StrUtil.equalsAnyIgnoreCase(firstText(root, "status"), "ok", "healthy", "up");
        Map<String, Object> result = toMap(root);
        result.put("ok", ok);
        result.put("baseUrl", settings.baseUrl());
        return result;
    }

    private Map<String, Object> buildSubmitRequest(AiMediaTaskDO task) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("taskId", Objects.toString(task.getId(), null));
        request.put("taskType", taskTypeName(task.getTaskType()));
        request.put("model", task.getModel());
        request.put("input", task.getInputJson());
        request.put("sourceFiles", buildSourceFiles(task));
        request.put("callbackUrl", callbackUrl);
        request.put("tenantId", Objects.toString(task.getTenantId(), null));
        request.put("userId", Objects.toString(task.getUserId(), null));
        request.put("traceId", UUID.randomUUID().toString());
        return request;
    }

    private List<Map<String, Object>> buildSourceFiles(AiMediaTaskDO task) {
        List<Map<String, Object>> sourceFiles = new ArrayList<>();
        if (task.getInputJson() != null && task.getInputJson().get("sourceAsset") instanceof Map<?, ?> sourceAsset) {
            Map<String, Object> file = new LinkedHashMap<>();
            sourceAsset.forEach((key, value) -> file.put(Objects.toString(key), value));
            sourceFiles.add(file);
            return sourceFiles;
        }
        if (task.getSourceFileIds() != null) {
            for (String sourceFileId : task.getSourceFileIds()) {
                sourceFiles.add(Map.of("fileId", sourceFileId, "url", sourceFileId));
            }
        }
        return sourceFiles;
    }

    private AiMediaProviderQueryResult mapStatus(JsonNode root) {
        String workerStatus = firstText(root, "status", "taskStatus", "task_status");
        Map<String, Object> rawOutput = toMap(root);
        if (StrUtil.equalsIgnoreCase(workerStatus, "SUCCEEDED")) {
            JsonNode output = root.at("/outputs/0");
            String resultUrl = firstText(output, "url", "fileUrl", "file_url");
            if (StrUtil.isBlank(resultUrl)) {
                resultUrl = firstText(root, "url", "resultUrl", "result_url");
            }
            if (StrUtil.isBlank(resultUrl)) {
                throw new AiMediaProviderException("INVALID_RESPONSE", "Python Worker 成功响应未返回结果 URL");
            }
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), 100,
                    "worker_succeeded", resultUrl, StrUtil.blankToDefault(firstText(output, "mimeType", "mime_type"),
                    "video/mp4"), null, null, rawOutput);
        }
        if (StrUtil.equalsIgnoreCase(workerStatus, "FAILED")) {
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.FAILED.getStatus(), 100,
                    "worker_failed", null, null, StrUtil.blankToDefault(firstText(root, "errorCode", "error_code"),
                    "WORKER_FAILED"), StrUtil.blankToDefault(firstText(root, "errorMessage", "error_message", "message"),
                    "Python Worker 执行失败"), rawOutput);
        }
        if (StrUtil.equalsIgnoreCase(workerStatus, "CANCELLED")) {
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.CANCELLED.getStatus(), 100,
                    "worker_cancelled", null, null, null, null, rawOutput);
        }
        if (StrUtil.equalsAnyIgnoreCase(workerStatus, "QUEUED", "SUBMITTED")) {
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.QUEUED.getStatus(),
                    root.path("progress").asInt(10), "worker_queueing", null, null, null, null, rawOutput);
        }
        if (StrUtil.equalsAnyIgnoreCase(workerStatus, "RUNNING", "RETRYING", "CANCELLING")) {
            return new AiMediaProviderQueryResult(AiMediaTaskStatusEnum.RUNNING.getStatus(),
                    root.path("progress").asInt(50), StrUtil.blankToDefault(firstText(root, "stage"), "worker_running"),
                    null, null, null, null, rawOutput);
        }
        throw new AiMediaProviderException("UNKNOWN_STATUS",
                StrUtil.format("Python Worker 任务状态无法识别：{}", workerStatus));
    }

    private WorkerSettings resolveSettings(AiMediaTaskDO task) {
        if (Boolean.TRUE.equals(enable)) {
            return resolveConfiguredSettings();
        }
        AiApiKeyDO apiKey = resolveApiKey(task);
        if (StrUtil.isBlank(apiKey.getUrl())) {
            throw new AiMediaProviderException("WORKER_CONFIG_MISSING",
                    "PythonWorker API Key 的 URL 未配置，请填写 http://127.0.0.1:8099");
        }
        String token = StrUtil.blankToDefault(apiKey.getApiKey(), authToken);
        if (StrUtil.isBlank(token)) {
            throw new AiMediaProviderException("WORKER_CONFIG_MISSING",
                    "PythonWorker API Key 未配置，请填写 worker-secret");
        }
        return new WorkerSettings(apiKey.getUrl(), token);
    }

    private WorkerSettings resolveConfiguredSettings() {
        if (StrUtil.isBlank(baseUrl)) {
            throw new AiMediaProviderException("WORKER_CONFIG_MISSING", "Python Worker base URL 未配置");
        }
        if (StrUtil.isBlank(authToken)) {
            throw new AiMediaProviderException("WORKER_CONFIG_MISSING", "Python Worker auth token 未配置");
        }
        return new WorkerSettings(baseUrl, authToken);
    }

    private AiApiKeyDO resolveApiKey(AiMediaTaskDO task) {
        AiModelDO model = modelService.validateModel(task.getModelId());
        if (!Objects.equals(AiPlatformEnum.PYTHON_WORKER.getPlatform(), model.getPlatform())) {
            throw new AiMediaProviderException("MODEL_PLATFORM_MISMATCH", "PythonWorker 视频模型平台不匹配");
        }
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        if (!CommonStatusEnum.ENABLE.getStatus().equals(apiKey.getStatus())) {
            throw new AiMediaProviderException("DISABLED_API_KEY", "PythonWorker API Key 已禁用");
        }
        return apiKey;
    }

    private String doGet(WorkerSettings settings, String path) {
        try (HttpResponse response = withAuthorization(HttpRequest.get(buildUrl(settings, path)), settings)
                .timeout(resolveTimeoutMillis()).execute()) {
            return checkedBody(response);
        } catch (AiMediaProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiMediaProviderException("WORKER_NETWORK_ERROR", "调用 Python Worker 失败", e);
        }
    }

    private String doPost(WorkerSettings settings, String path, String requestBody) {
        try (HttpResponse response = withAuthorization(HttpRequest.post(buildUrl(settings, path)), settings)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .timeout(resolveTimeoutMillis()).execute()) {
            return checkedBody(response);
        } catch (AiMediaProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiMediaProviderException("WORKER_NETWORK_ERROR", "调用 Python Worker 失败", e);
        }
    }

    private HttpRequest withAuthorization(HttpRequest request, WorkerSettings settings) {
        return request.header("Authorization", "Bearer " + settings.authToken());
    }

    private String checkedBody(HttpResponse response) {
        if (response.getStatus() < HttpStatus.HTTP_OK || response.getStatus() >= HttpStatus.HTTP_MULT_CHOICE) {
            throw new AiMediaProviderException("WORKER_HTTP_" + response.getStatus(),
                    StrUtil.maxLength(response.body(), 480));
        }
        return response.body();
    }

    private String buildUrl(WorkerSettings settings, String path) {
        return StrUtil.removeSuffix(settings.baseUrl(), "/") + path;
    }

    private int resolveTimeoutMillis() {
        return requestTimeoutMillis == null || requestTimeoutMillis <= 0 ? 30_000 : requestTimeoutMillis;
    }

    private String taskTypeName(Integer taskType) {
        for (AiMediaTaskTypeEnum value : AiMediaTaskTypeEnum.values()) {
            if (Objects.equals(value.getType(), taskType)) {
                return value.name();
            }
        }
        return AiMediaTaskTypeEnum.UNKNOWN.name();
    }

    private Map<String, Object> toMap(JsonNode root) {
        return objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
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

    private record WorkerSettings(String baseUrl, String authToken) {
    }

}
