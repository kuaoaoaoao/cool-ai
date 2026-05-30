package cn.iocoder.yudao.module.ai.service.media.provider.worker;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapter;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderException;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderQueryResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderSubmitResult;
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
        return Boolean.TRUE.equals(enable)
                && Objects.equals(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(), taskType)
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
        validateConfiguration();
        String body = doPost(TASKS_PATH, objectMapper.writeValueAsString(buildSubmitRequest(task)));
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
        validateConfiguration();
        if (StrUtil.isBlank(task.getProviderTaskId())) {
            throw new AiMediaProviderException("MISSING_WORKER_TASK_ID", "Python Worker taskId 不能为空");
        }
        return mapStatus(objectMapper.readTree(doGet(TASKS_PATH + "/" + task.getProviderTaskId())));
    }

    @Override
    public void cancel(AiMediaTaskDO task) {
        validateConfiguration();
        if (StrUtil.isBlank(task.getProviderTaskId())) {
            throw new AiMediaProviderException("MISSING_WORKER_TASK_ID", "Python Worker taskId 不能为空");
        }
        doPost(TASKS_PATH + "/" + task.getProviderTaskId() + "/cancel", "{}");
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
        validateConfiguration();
        JsonNode root = objectMapper.readTree(doGet(HEALTH_PATH));
        return root.path("ok").asBoolean(false)
                || StrUtil.equalsAnyIgnoreCase(firstText(root, "status"), "ok", "healthy", "up");
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

    private void validateConfiguration() {
        if (!Boolean.TRUE.equals(enable)) {
            throw new AiMediaProviderException("WORKER_DISABLED", "Python Worker 未启用");
        }
        if (StrUtil.isBlank(baseUrl)) {
            throw new AiMediaProviderException("WORKER_CONFIG_MISSING", "Python Worker base URL 未配置");
        }
        if (StrUtil.isBlank(authToken)) {
            throw new AiMediaProviderException("WORKER_CONFIG_MISSING", "Python Worker auth token 未配置");
        }
    }

    private String doGet(String path) {
        try (HttpResponse response = withAuthorization(HttpRequest.get(buildUrl(path)))
                .timeout(resolveTimeoutMillis()).execute()) {
            return checkedBody(response);
        } catch (AiMediaProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiMediaProviderException("WORKER_NETWORK_ERROR", "调用 Python Worker 失败", e);
        }
    }

    private String doPost(String path, String requestBody) {
        try (HttpResponse response = withAuthorization(HttpRequest.post(buildUrl(path)))
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

    private HttpRequest withAuthorization(HttpRequest request) {
        return request.header("Authorization", "Bearer " + authToken);
    }

    private String checkedBody(HttpResponse response) {
        if (response.getStatus() < HttpStatus.HTTP_OK || response.getStatus() >= HttpStatus.HTTP_MULT_CHOICE) {
            throw new AiMediaProviderException("WORKER_HTTP_" + response.getStatus(),
                    StrUtil.maxLength(response.body(), 480));
        }
        return response.body();
    }

    private String buildUrl(String path) {
        return StrUtil.removeSuffix(baseUrl, "/") + path;
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

}
