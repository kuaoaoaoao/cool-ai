package cn.iocoder.yudao.module.ai.service.media;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaImageToVideoCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiVoiceProfileDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiMediaTaskMapper;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiVoiceProfileMapper;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiModelCapabilities;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapter;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapterResolver;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderException;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderQueryResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderSubmitResult;
import cn.iocoder.yudao.module.ai.service.media.provider.xiaomi.XiaomiMiMoTtsProviderAdapter;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class AiMediaTaskServiceImpl implements AiMediaTaskService, AiMediaExecutionService {

    @Resource
    private AiMediaTaskMapper mediaTaskMapper;
    @Resource
    private AiVoiceProfileMapper voiceProfileMapper;
    @Resource
    private AiModelService modelService;
    @Resource
    private AiMediaProviderAdapterResolver providerAdapterResolver;
    @Resource
    private FileApi fileApi;
    @Resource
    private AiRemoteResourceService remoteResourceService;

    @Value("${yudao.ai.media.tts.enable:true}")
    private Boolean ttsEnable;
    @Value("${yudao.ai.media.tts.user-daily-limit:100}")
    private Integer ttsUserDailyLimit;
    @Value("${yudao.ai.media.tts.quota-lock-strict:true}")
    private Boolean ttsQuotaLockStrict;
    @Value("${yudao.ai.media.video.enable:true}")
    private Boolean videoEnable;
    @Value("${yudao.ai.media.video.user-daily-limit:20}")
    private Integer videoUserDailyLimit;
    @Value("${yudao.ai.media.video.tenant-daily-limit:200}")
    private Integer videoTenantDailyLimit;
    @Value("${yudao.ai.media.video.user-concurrent-limit:3}")
    private Integer videoUserConcurrentLimit;
    @Value("${yudao.ai.media.video.max-retry-count:2}")
    private Integer videoMaxRetryCount;
    @Value("${yudao.ai.media.video.max-duration-seconds:10}")
    private Integer videoMaxDurationSeconds;
    @Value("${yudao.ai.media.video.max-source-bytes:10485760}")
    private Long videoMaxSourceBytes;
    @Value("${yudao.ai.media.video.max-result-bytes:209715200}")
    private Long videoMaxResultBytes;
    @Value("${yudao.ai.media.video.poll-limit:20}")
    private Integer videoPollLimit;
    @Value("${yudao.ai.media.worker.poll-limit:20}")
    private Integer workerPollLimit;
    @Value("${yudao.ai.media.asset.retention-days:30}")
    private Integer assetRetentionDays;

    private static final String DEFAULT_VIDEO_IMAGE_SIZE = "1280x720";
    private static final Set<String> SUPPORTED_VIDEO_IMAGE_SIZES = Set.of("1280x720", "720x1280", "960x960");
    private static final Set<String> SUPPORTED_IMAGE_MIME_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final List<Integer> RUNNING_VIDEO_STATUSES = List.of(
            AiMediaTaskStatusEnum.CREATED.getStatus(),
            AiMediaTaskStatusEnum.QUEUED.getStatus(),
            AiMediaTaskStatusEnum.SUBMITTED.getStatus(),
            AiMediaTaskStatusEnum.RUNNING.getStatus(),
            AiMediaTaskStatusEnum.RETRYING.getStatus(),
            AiMediaTaskStatusEnum.CANCELLING.getStatus());

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTtsTask(Long userId, AiMediaTtsCreateReqVO reqVO) {
        validateTtsEnabled(userId);
        RLock lock = tryLockTtsQuota(userId);
        boolean unlockRegistered = false;
        try {
            validateTtsQuota(userId);
            reqVO = applyVoiceProfile(userId, reqVO);
            validateTtsCreateReqVO(reqVO);
            AiVoiceProfileSourceTypeEnum voiceMode = parseVoiceMode(reqVO.getVoiceMode());
            AiModelDO model = validateTtsModel(reqVO.getModelId(), voiceMode);

            AiMediaTaskDO task = new AiMediaTaskDO()
                    .setUserId(userId)
                    .setTaskType(AiMediaTaskTypeEnum.TTS.getType())
                    .setPlatform(model.getPlatform())
                    .setModelId(model.getId())
                    .setModel(model.getModel())
                    .setStatus(AiMediaTaskStatusEnum.CREATED.getStatus())
                    .setProgress(0)
                    .setStage("created")
                    .setInputJson(buildTtsInput(reqVO))
                    .setSourceFileIds(buildTtsSourceFileIds(reqVO, voiceMode));
            mediaTaskMapper.insert(task);

            scheduleTtsTask(task);
            unlockTtsQuotaAfterTransaction(lock);
            unlockRegistered = true;
            return task.getId();
        } finally {
            if (!unlockRegistered) {
                unlockQuietly(lock);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createImageToVideoTask(Long userId, AiMediaImageToVideoCreateReqVO reqVO) {
        validateVideoEnabled();
        validateImageToVideoCreateReqVO(reqVO);
        validateImageToVideoSource(userId, reqVO);
        validateVideoQuota(userId);
        validateVideoTenantQuota();
        validateVideoConcurrency(userId);
        AiModelDO model = validateVideoModel(reqVO.getModelId());

        AiMediaTaskDO task = new AiMediaTaskDO()
                .setUserId(userId)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(model.getPlatform())
                .setModelId(model.getId())
                .setModel(model.getModel())
                .setStatus(AiMediaTaskStatusEnum.QUEUED.getStatus())
                .setProgress(0)
                .setStage("queued")
                .setInputJson(buildImageToVideoInput(reqVO))
                .setSourceFileIds(List.of(reqVO.getSourceImageUrl()));
        mediaTaskMapper.insert(task);

        enqueueTask(task);
        return task.getId();
    }

    @Override
    public Long createTask(Long userId, AiMediaTaskCreateReqVO reqVO) {
        if (AiMediaTaskTypeEnum.TTS.getType().equals(reqVO.getTaskType())) {
            AiMediaTtsCreateReqVO ttsReqVO = JSONUtil.toBean(JSONUtil.toJsonStr(reqVO.getInputJson()),
                    AiMediaTtsCreateReqVO.class);
            return createTtsTask(userId, ttsReqVO);
        }
        if (AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType().equals(reqVO.getTaskType())) {
            AiMediaImageToVideoCreateReqVO videoReqVO = JSONUtil.toBean(JSONUtil.toJsonStr(reqVO.getInputJson()),
                    AiMediaImageToVideoCreateReqVO.class);
            return createImageToVideoTask(userId, videoReqVO);
        }
        throw exception(MEDIA_TASK_TYPE_NOT_ENABLE, reqVO.getTaskType());
    }

    @Override
    public AiMediaTaskDO getTask(Long id) {
        return mediaTaskMapper.selectById(id);
    }

    @Override
    public AiMediaTaskDO getUserTask(Long id, Long userId) {
        AiMediaTaskDO task = validateTaskExists(id);
        if (ObjUtil.notEqual(task.getUserId(), userId)) {
            throw exception(MEDIA_TASK_FORBIDDEN);
        }
        return task;
    }

    @Override
    public AiMediaTaskDO getUserTaskAndRefresh(Long id, Long userId) {
        AiMediaTaskDO task = getUserTask(id, userId);
        if (shouldRefreshImageToVideoTask(task)) {
            try {
                refreshTask(task);
            } catch (Exception e) {
                log.warn("[getUserTaskAndRefresh][taskId({}) 刷新图生视频状态失败]", id, e);
            }
            return getUserTask(id, userId);
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryUserTask(Long id, Long userId) {
        retryTask(id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Long id, Long userId) {
        AiMediaTaskDO task = getUserTask(id, userId);
        if (!AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType().equals(task.getTaskType())) {
            throw exception(MEDIA_TASK_TYPE_NOT_ENABLE, task.getTaskType());
        }
        if (!AiMediaTaskStatusEnum.FAILED.getStatus().equals(task.getStatus())) {
            throw exception(MEDIA_TASK_STATUS_INVALID, task.getStatus());
        }
        int retryCount = getRetryCount(task);
        int maxRetryCount = videoMaxRetryCount == null || videoMaxRetryCount < 0 ? 2 : videoMaxRetryCount;
        if (retryCount >= maxRetryCount) {
            throw exception(MEDIA_TASK_RETRY_EXCEEDED);
        }

        Map<String, Object> outputJson = mergeOutputJson(task.getOutputJson());
        outputJson.put("executionMode", "PROVIDER_ASYNC");
        outputJson.put("retryCount", retryCount + 1);
        outputJson.put("lastRetryTime", LocalDateTime.now().toString());
        outputJson.remove("cancelRequested");
        outputJson.remove("cancelRequestedTime");

        mediaTaskMapper.updateById(new AiMediaTaskDO()
                .setId(id)
                .setStatus(AiMediaTaskStatusEnum.RETRYING.getStatus())
                .setProgress(0)
                .setStage("retrying")
                .setProviderTaskId("")
                .setErrorCode("")
                .setErrorMessage("")
                .setOutputJson(outputJson));

        scheduleImageToVideoTask(task
                .setProviderTaskId("")
                .setStatus(AiMediaTaskStatusEnum.RETRYING.getStatus())
                .setProgress(0)
                .setStage("retrying")
                .setOutputJson(outputJson));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelUserTask(Long id, Long userId) {
        cancelTask(id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long id, Long userId) {
        AiMediaTaskDO task = getUserTask(id, userId);
        if (AiMediaTaskStatusEnum.isTerminal(task.getStatus())) {
            throw exception(MEDIA_TASK_STATUS_INVALID, task.getStatus());
        }
        if (StrUtil.isBlank(task.getProviderTaskId())) {
            updateTaskCancelled(task, "cancelled_before_submit", mergeCancelOutput(task, false));
            return;
        }

        Map<String, Object> outputJson = mergeCancelOutput(task, false);
        try {
            AiMediaProviderAdapter adapter = providerAdapterResolver.resolve(task);
            if (adapter != null) {
                adapter.cancel(task);
                outputJson.put("providerCancelled", true);
            }
        } catch (UnsupportedOperationException e) {
            outputJson.put("cancelProviderUnsupported", true);
        } catch (Exception e) {
            outputJson.put("cancelProviderError", StrUtil.maxLength(e.getMessage(), 240));
        }
        mediaTaskMapper.updateById(new AiMediaTaskDO()
                .setId(id)
                .setStatus(AiMediaTaskStatusEnum.CANCELLING.getStatus())
                .setProgress(task.getProgress())
                .setStage("cancelling")
                .setOutputJson(outputJson));
    }

    @Override
    public PageResult<AiMediaTaskDO> getTaskPage(AiMediaTaskPageReqVO pageReqVO) {
        return mediaTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AiMediaTaskDO> getUserTaskPage(AiMediaTaskPageReqVO pageReqVO, Long userId) {
        return mediaTaskMapper.selectPageByUser(pageReqVO, userId);
    }

    @Override
    public PageResult<AiMediaAssetRespVO> getAssetPage(AiMediaAssetPageReqVO pageReqVO) {
        List<AiMediaAssetRespVO> assets = buildAssetList(mediaTaskMapper.selectListForAssetPage(pageReqVO), pageReqVO);
        return buildAssetPageResult(assets, pageReqVO);
    }

    @Override
    public PageResult<AiMediaAssetRespVO> getUserAssetPage(AiMediaAssetPageReqVO pageReqVO, Long userId) {
        pageReqVO.setUserId(userId);
        List<AiMediaAssetRespVO> assets = buildAssetList(mediaTaskMapper.selectListForAssetPage(pageReqVO), pageReqVO);
        return buildAssetPageResult(assets, pageReqVO);
    }

    @Override
    public List<AiMediaAssetRespVO> getTaskAssets(Long taskId) {
        return buildTaskAssets(validateTaskExists(taskId));
    }

    @Override
    public List<AiMediaAssetRespVO> getUserTaskAssets(Long taskId, Long userId) {
        return buildTaskAssets(getUserTask(taskId, userId));
    }

    @Override
    public Integer syncSiliconFlowImageToVideoTasks() {
        return syncImageToVideoTasks(AiPlatformEnum.SILICON_FLOW.getPlatform(), videoPollLimit);
    }

    @Override
    public Integer syncPythonWorkerImageToVideoTasks() {
        return syncImageToVideoTasks(AiPlatformEnum.PYTHON_WORKER.getPlatform(), workerPollLimit);
    }

    private Integer syncImageToVideoTasks(String platform, Integer pollLimit) {
        List<AiMediaTaskDO> tasks = mediaTaskMapper
                .selectListByTaskTypeAndPlatformAndStatusInAndProviderTaskIdNotEmpty(
                        AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(),
                        platform,
                        List.of(AiMediaTaskStatusEnum.QUEUED.getStatus(),
                                AiMediaTaskStatusEnum.SUBMITTED.getStatus(),
                                AiMediaTaskStatusEnum.RUNNING.getStatus(),
                                AiMediaTaskStatusEnum.RETRYING.getStatus(),
                                AiMediaTaskStatusEnum.CANCELLING.getStatus()),
                        pollLimit == null || pollLimit <= 0 ? 20 : pollLimit);
        int count = 0;
        for (AiMediaTaskDO task : tasks) {
            try {
                refreshTask(task);
                count++;
            } catch (Exception e) {
                log.warn("[syncImageToVideoTasks][platform({}) taskId({}) 同步失败]", platform, task.getId(), e);
            }
        }
        return count;
    }

    @Override
    public void handlePythonWorkerCallback(String token, Map<String, Object> payload) {
        Long taskId = toLong(payload == null ? null : payload.get("taskId"));
        if (taskId == null) {
            throw new AiMediaProviderException("INVALID_CALLBACK", "Python Worker 回调缺少 taskId");
        }
        AiMediaTaskDO task = validateTaskExists(taskId);
        if (!AiPlatformEnum.PYTHON_WORKER.getPlatform().equals(task.getPlatform())) {
            throw new AiMediaProviderException("INVALID_CALLBACK", "媒体任务不是 Python Worker 任务");
        }
        AiMediaProviderAdapter adapter = providerAdapterResolver.resolve(task);
        if (adapter == null) {
            throw exception(MEDIA_PROVIDER_NOT_EXISTS);
        }
        if (!adapter.verifyCallbackToken(token)) {
            throw new AiMediaProviderException("INVALID_CALLBACK_TOKEN", "Python Worker 回调 Token 无效");
        }
        AiMediaProviderQueryResult queryResult = adapter.handleCallback(task, payload);
        applyImageToVideoStatus(task, queryResult, "workerCallback");
    }

    @Override
    public void enqueueTask(AiMediaTaskDO task) {
        scheduleImageToVideoTask(task);
    }

    @Override
    public void submitTask(AiMediaTaskDO task) {
        getSelf().submitImageToVideoTask(task);
    }

    @Override
    public void refreshTask(AiMediaTaskDO task) {
        syncImageToVideoTask(task);
    }

    @Override
    public void completeTask(AiMediaTaskDO task, AiMediaProviderQueryResult queryResult) {
        completeImageToVideoTask(task, queryResult);
    }

    @Override
    public void updateTaskRunning(Long id, String stage) {
        mediaTaskMapper.updateById(new AiMediaTaskDO()
                .setId(id)
                .setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus())
                .setProgress(50)
                .setStage(stage));
    }

    @Override
    public void updateTaskSucceeded(Long id, String stage, String fileUrl, String mimeType, Object providerOutput) {
        AiMediaTaskDO current = mediaTaskMapper.selectById(id);
        Map<String, Object> outputJson = new LinkedHashMap<>();
        outputJson.put("fileId", fileUrl);
        outputJson.put("url", fileUrl);
        outputJson.put("mimeType", mimeType);
        outputJson.put("providerOutput", providerOutput);
        outputJson.put("usageRecord", buildUsageRecord(current, AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), null,
                null, null));
        mediaTaskMapper.updateById(new AiMediaTaskDO()
                .setId(id)
                .setStatus(AiMediaTaskStatusEnum.SUCCEEDED.getStatus())
                .setProgress(100)
                .setStage(stage)
                .setOutputJson(outputJson)
                .setResultFileIds(List.of(fileUrl))
                .setFinishTime(LocalDateTime.now()));
    }

    @Override
    public void updateTaskFailed(Long id, String errorCode, String errorMessage) {
        updateTaskFailed(id, errorCode, errorMessage, null);
    }

    private void updateTaskFailed(Long id, String errorCode, String errorMessage,
                                  Map<String, Object> providerOutputJson) {
        AiMediaTaskDO current = mediaTaskMapper.selectById(id);
        if (current != null && AiMediaTaskStatusEnum.isTerminal(current.getStatus())) {
            return;
        }
        Map<String, Object> outputJson = mergeOutputJson(providerOutputJson != null ? providerOutputJson
                : current == null ? null : current.getOutputJson());
        outputJson.put("usageRecord", buildUsageRecord(current, AiMediaTaskStatusEnum.FAILED.getStatus(), errorCode,
                null, null));
        mediaTaskMapper.updateById(new AiMediaTaskDO()
                .setId(id)
                .setStatus(AiMediaTaskStatusEnum.FAILED.getStatus())
                .setProgress(100)
                .setStage("failed")
                .setErrorCode(StrUtil.maxLength(errorCode, 64))
                .setErrorMessage(StrUtil.maxLength(errorMessage, 480))
                .setOutputJson(outputJson)
                .setFinishTime(LocalDateTime.now()));
    }

    private void scheduleTtsTask(AiMediaTaskDO task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            getSelf().executeTtsTask(task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                getSelf().executeTtsTask(task);
            }

        });
    }

    private void scheduleImageToVideoTask(AiMediaTaskDO task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            getSelf().submitImageToVideoTask(task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                getSelf().submitImageToVideoTask(task);
            }

        });
    }

    @Async
    public void executeTtsTask(AiMediaTaskDO task) {
        updateTaskRunning(task.getId(), "calling_provider");
        task.setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus()).setProgress(50).setStage("calling_provider");
        try {
            AiMediaProviderAdapter adapter = providerAdapterResolver.resolve(task);
            if (adapter == null) {
                throw exception(MEDIA_PROVIDER_NOT_EXISTS);
            }
            AiMediaProviderResult providerResult = adapter.execute(task);
            String fileUrl = saveProviderResult(task.getId(), providerResult);
            updateTaskSucceeded(task.getId(), "done", fileUrl, providerResult.getMimeType(), providerResult.getRawOutput());
        } catch (AiMediaProviderException e) {
            log.warn("[executeTtsTask][taskId({}) Provider 调用失败：{}]", task.getId(), e.getMessage(), e);
            updateTaskFailed(task.getId(), e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("[executeTtsTask][taskId({}) TTS 执行失败]", task.getId(), e);
            updateTaskFailed(task.getId(), "EXECUTE_FAIL", e.getMessage());
        }
    }

    @Async
    public void submitImageToVideoTask(AiMediaTaskDO task) {
        if (!guardedUpdateTaskProgress(task.getId(), AiMediaTaskStatusEnum.RUNNING.getStatus(), 2,
                "submitting", null)) {
            return;
        }
        task.setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus()).setProgress(2).setStage("submitting");
        try {
            AiMediaProviderAdapter adapter = providerAdapterResolver.resolve(task);
            if (adapter == null) {
                throw exception(MEDIA_PROVIDER_NOT_EXISTS);
            }
            AiMediaProviderSubmitResult submitResult = adapter.submit(task);
            Map<String, Object> outputJson = mergeOutputJson(task.getOutputJson());
            outputJson.put("providerSubmit", submitResult.getRawOutput());
            outputJson.put("executionMode", "PROVIDER_ASYNC");
            outputJson.put("submittedTime", LocalDateTime.now().toString());
            outputJson.putIfAbsent("retryCount", 0);
            outputJson.putIfAbsent("callbackStatus", "NONE");
            guardedUpdateTaskSubmitted(task.getId(), submitResult, outputJson);
        } catch (AiMediaProviderException e) {
            log.warn("[submitImageToVideoTask][taskId({}) Provider 提交失败：{}]", task.getId(), e.getMessage(), e);
            updateTaskFailed(task.getId(), e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("[submitImageToVideoTask][taskId({}) 图生视频提交失败]", task.getId(), e);
            updateTaskFailed(task.getId(), "SUBMIT_FAIL", e.getMessage());
        }
    }

    /**
     * 获得自身的代理对象，保证 @Async 生效；单元测试没有 Spring 容器时回退到当前对象。
     */
    private AiMediaTaskServiceImpl getSelf() {
        try {
            return SpringUtil.getBean(getClass());
        } catch (Exception e) {
            return this;
        }
    }

    private String saveProviderResult(Long taskId, AiMediaProviderResult providerResult) {
        try {
            return fileApi.createFile(providerResult.getContent(), providerResult.getFileName(),
                    "ai-media/tts", providerResult.getMimeType());
        } catch (Exception e) {
            int contentLength = providerResult.getContent() == null ? 0 : providerResult.getContent().length;
            String message = StrUtil.format("保存 TTS 音频文件失败：{}，fileName={}，mimeType={}，contentLength={}",
                    e.getMessage(), providerResult.getFileName(), providerResult.getMimeType(), contentLength);
            log.warn("[saveProviderResult][taskId({}) {}]", taskId, message, e);
            throw new AiMediaProviderException("FILE_SAVE_FAIL", message, e);
        }
    }

    private void syncImageToVideoTask(AiMediaTaskDO task) {
        if (isTerminalTask(task.getId())) {
            return;
        }
        AiMediaProviderAdapter adapter = providerAdapterResolver.resolve(task);
        if (adapter == null) {
            throw exception(MEDIA_PROVIDER_NOT_EXISTS);
        }
        AiMediaProviderQueryResult queryResult = adapter.query(task);
        applyImageToVideoStatus(task, queryResult, "providerStatus");
    }

    private void applyImageToVideoStatus(AiMediaTaskDO task, AiMediaProviderQueryResult queryResult,
                                         String rawOutputKey) {
        if (isTerminalTask(task.getId())) {
            return;
        }
        Map<String, Object> outputJson = mergeOutputJson(task.getOutputJson());
        outputJson.put(rawOutputKey, queryResult.getRawOutput());
        if ("workerCallback".equals(rawOutputKey)) {
            outputJson.put("callbackStatus", "RECEIVED");
            outputJson.put("callbackReceivedTime", LocalDateTime.now().toString());
        }
        task.setOutputJson(outputJson);
        if (isCancelRequested(task)) {
            if (AiMediaTaskStatusEnum.SUCCEEDED.getStatus().equals(queryResult.getStatus())
                    || AiMediaTaskStatusEnum.FAILED.getStatus().equals(queryResult.getStatus())) {
                updateTaskCancelled(task, "cancelled_after_provider_terminal", outputJson);
                return;
            }
            guardedUpdateTaskProgress(task.getId(), AiMediaTaskStatusEnum.CANCELLING.getStatus(),
                    task.getProgress(), "cancelling", outputJson);
            return;
        }
        if (AiMediaTaskStatusEnum.SUCCEEDED.getStatus().equals(queryResult.getStatus())) {
            completeTask(task, queryResult);
            return;
        }
        if (AiMediaTaskStatusEnum.FAILED.getStatus().equals(queryResult.getStatus())) {
            updateTaskFailed(task.getId(), queryResult.getErrorCode(), queryResult.getErrorMessage(), outputJson);
            return;
        }
        if (AiMediaTaskStatusEnum.CANCELLED.getStatus().equals(queryResult.getStatus())) {
            updateTaskCancelled(task, "cancelled_by_provider", outputJson);
            return;
        }
        guardedUpdateTaskProgress(task.getId(), queryResult.getStatus(), queryResult.getProgress(),
                queryResult.getStage(), outputJson);
    }

    private void completeImageToVideoTask(AiMediaTaskDO task, AiMediaProviderQueryResult queryResult) {
        try {
            if (isTerminalTask(task.getId())) {
                return;
            }
            byte[] video = remoteResourceService.download(queryResult.getResultUrl(), videoMaxResultBytes);
            String mimeType = StrUtil.blankToDefault(queryResult.getResultMimeType(), "video/mp4");
            if (isTerminalTask(task.getId())) {
                return;
            }
            String fileUrl = fileApi.createFile(video, "video-" + task.getId() + ".mp4", "ai-media/video", mimeType);
            Map<String, Object> outputJson = mergeOutputJson(task.getOutputJson());
            outputJson.put("providerStatus", queryResult.getRawOutput());
            outputJson.put("resultAsset", buildResultVideoAsset(task, fileUrl, mimeType, video.length));
            outputJson.put("usageRecord", buildUsageRecord(task, AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), null,
                    (long) video.length, mimeType));
            mediaTaskMapper.updateById(new AiMediaTaskDO()
                    .setId(task.getId())
                    .setStatus(AiMediaTaskStatusEnum.SUCCEEDED.getStatus())
                    .setProgress(100)
                    .setStage("done")
                    .setOutputJson(outputJson)
                    .setResultFileIds(List.of(fileUrl))
                    .setFinishTime(LocalDateTime.now()));
        } catch (Exception e) {
            String message = StrUtil.format("保存视频结果失败：{}", e.getMessage());
            log.warn("[completeImageToVideoTask][taskId({}) {}]", task.getId(), message, e);
            updateTaskFailed(task.getId(), "FILE_SAVE_FAIL", message);
        }
    }

    private void guardedUpdateTaskSubmitted(Long id, AiMediaProviderSubmitResult submitResult,
                                            Map<String, Object> outputJson) {
        AiMediaTaskDO current = mediaTaskMapper.selectById(id);
        if (current == null || AiMediaTaskStatusEnum.isTerminal(current.getStatus())) {
            return;
        }
        mediaTaskMapper.updateById(new AiMediaTaskDO()
                .setId(id)
                .setStatus(submitResult.getStatus())
                .setProgress(submitResult.getProgress())
                .setStage(submitResult.getStage())
                .setProviderTaskId(submitResult.getProviderTaskId())
                .setOutputJson(outputJson));
    }

    private boolean guardedUpdateTaskProgress(Long id, Integer status, Integer progress, String stage,
                                              Map<String, Object> outputJson) {
        AiMediaTaskDO current = mediaTaskMapper.selectById(id);
        if (current == null || AiMediaTaskStatusEnum.isTerminal(current.getStatus())) {
            return false;
        }
        if (AiMediaTaskStatusEnum.CANCELLING.getStatus().equals(current.getStatus())
                && !AiMediaTaskStatusEnum.CANCELLING.getStatus().equals(status)) {
            return false;
        }
        AiMediaTaskDO updateObj = new AiMediaTaskDO()
                .setId(id)
                .setStatus(status)
                .setProgress(progress)
                .setStage(stage);
        if (outputJson != null) {
            updateObj.setOutputJson(outputJson);
        }
        mediaTaskMapper.updateById(updateObj);
        return true;
    }

    private boolean isTerminalTask(Long id) {
        AiMediaTaskDO current = mediaTaskMapper.selectById(id);
        return current != null && AiMediaTaskStatusEnum.isTerminal(current.getStatus());
    }

    private boolean shouldRefreshImageToVideoTask(AiMediaTaskDO task) {
        return task != null
                && AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType().equals(task.getTaskType())
                && (AiPlatformEnum.SILICON_FLOW.getPlatform().equals(task.getPlatform())
                || AiPlatformEnum.PYTHON_WORKER.getPlatform().equals(task.getPlatform()))
                && StrUtil.isNotBlank(task.getProviderTaskId())
                && !AiMediaTaskStatusEnum.isTerminal(task.getStatus());
    }

    private void validateTtsEnabled(Long userId) {
        if (!Boolean.TRUE.equals(ttsEnable)) {
            throw exception(MEDIA_TTS_DISABLED);
        }
    }

    private void validateVideoEnabled() {
        if (!Boolean.TRUE.equals(videoEnable)) {
            throw exception(MEDIA_VIDEO_DISABLED);
        }
    }

    private void validateTtsQuota(Long userId) {
        if (ttsUserDailyLimit == null || ttsUserDailyLimit <= 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        Long usedCount = mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(userId,
                AiMediaTaskTypeEnum.TTS.getType(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        if (usedCount >= ttsUserDailyLimit) {
            throw exception(MEDIA_TTS_QUOTA_EXCEEDED);
        }
    }

    private void validateVideoQuota(Long userId) {
        if (videoUserDailyLimit == null || videoUserDailyLimit <= 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        Long usedCount = mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(userId,
                AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        if (usedCount >= videoUserDailyLimit) {
            throw exception(MEDIA_VIDEO_QUOTA_EXCEEDED);
        }
    }

    private void validateVideoTenantQuota() {
        if (videoTenantDailyLimit == null || videoTenantDailyLimit <= 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        Long usedCount = mediaTaskMapper.selectCountByTaskTypeAndCreateTime(
                AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(), today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
        if (usedCount >= videoTenantDailyLimit) {
            throw exception(MEDIA_VIDEO_TENANT_QUOTA_EXCEEDED);
        }
    }

    private void validateVideoConcurrency(Long userId) {
        if (videoUserConcurrentLimit == null || videoUserConcurrentLimit <= 0) {
            return;
        }
        Long runningCount = mediaTaskMapper.selectCountByUserAndTaskTypeAndStatusIn(userId,
                AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(), RUNNING_VIDEO_STATUSES);
        if (runningCount >= videoUserConcurrentLimit) {
            throw exception(MEDIA_VIDEO_CONCURRENT_EXCEEDED);
        }
    }

    private RLock tryLockTtsQuota(Long userId) {
        if (ttsUserDailyLimit == null || ttsUserDailyLimit <= 0) {
            return null;
        }
        try {
            String lockKey = StrUtil.format("ai:media:tts-quota:{}:{}", userId, LocalDate.now());
            RLock lock = SpringUtil.getBean(RedissonClient.class).getLock(lockKey);
            lock.lock();
            return lock;
        } catch (Exception e) {
            log.warn("[tryLockTtsQuota][userId({}) 获取 TTS 配额锁失败]", userId, e);
            if (Boolean.TRUE.equals(ttsQuotaLockStrict)) {
                throw exception(MEDIA_TTS_QUOTA_LOCK_FAIL);
            }
            return null;
        }
    }

    private void unlockTtsQuotaAfterTransaction(RLock lock) {
        if (lock == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            unlockQuietly(lock);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int status) {
                unlockQuietly(lock);
            }

        });
    }

    private void unlockQuietly(RLock lock) {
        if (lock == null || !lock.isHeldByCurrentThread()) {
            return;
        }
        try {
            lock.unlock();
        } catch (Exception ignored) {
        }
    }

    private void validateTtsCreateReqVO(AiMediaTtsCreateReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getText())) {
            throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "TTS 合成文本不能为空");
        }
        if (StrUtil.isBlank(reqVO.getFormat())) {
            throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "TTS 音频格式不能为空");
        }
        AiVoiceProfileSourceTypeEnum voiceMode = parseVoiceMode(reqVO.getVoiceMode());
        switch (voiceMode) {
            case BUILTIN -> {
                if (StrUtil.isBlank(reqVO.getVoice())) {
                    throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "TTS 预置音色不能为空");
                }
            }
            case DESIGN -> {
                if (StrUtil.isBlank(reqVO.getDesignPrompt())) {
                    throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "文字设计音色描述不能为空");
                }
            }
            case CLONE -> {
                if (!Boolean.TRUE.equals(reqVO.getConsentConfirmed())) {
                    throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "音色克隆必须确认已获得声音授权");
                }
                if (StrUtil.isBlank(reqVO.getSampleFileUrl())) {
                    throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "音色克隆样本不能为空");
                }
            }
        }
    }

    private void validateImageToVideoCreateReqVO(AiMediaImageToVideoCreateReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getSourceImageUrl())) {
            throw exception(MEDIA_VIDEO_SOURCE_REQUIRED);
        }
        if (StrUtil.isBlank(reqVO.getPrompt())) {
            throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "图生视频提示词不能为空");
        }
        if (reqVO.getSourceSize() != null && videoMaxSourceBytes != null
                && videoMaxSourceBytes > 0 && reqVO.getSourceSize() > videoMaxSourceBytes) {
            throw exception(MEDIA_VIDEO_SOURCE_INVALID, "源图片大小超过限制");
        }
        if (StrUtil.isNotBlank(reqVO.getSourceMimeType())
                && !SUPPORTED_IMAGE_MIME_TYPES.contains(reqVO.getSourceMimeType().toLowerCase())) {
            throw exception(MEDIA_VIDEO_SOURCE_INVALID, "仅支持 jpg/png/webp 图片");
        }
        String imageSize = StrUtil.blankToDefault(reqVO.getImageSize(), DEFAULT_VIDEO_IMAGE_SIZE);
        if (!SUPPORTED_VIDEO_IMAGE_SIZES.contains(imageSize)) {
            throw exception(MEDIA_VIDEO_SIZE_INVALID, imageSize);
        }
        if (reqVO.getDuration() != null && videoMaxDurationSeconds != null && videoMaxDurationSeconds > 0
                && reqVO.getDuration() > videoMaxDurationSeconds) {
            throw exception(MEDIA_VIDEO_DURATION_INVALID, reqVO.getDuration());
        }
        reqVO.setImageSize(imageSize);
    }

    private void validateImageToVideoSource(Long userId, AiMediaImageToVideoCreateReqVO reqVO) {
        if (!isProjectFileUrl(reqVO.getSourceImageUrl())) {
            return;
        }
        FileRespDTO file = getSourceFile(reqVO.getSourceImageUrl());
        if (StrUtil.isBlank(file.getCreator()) || ObjUtil.notEqual(file.getCreator(), String.valueOf(userId))) {
            throw exception(MEDIA_VIDEO_SOURCE_FORBIDDEN);
        }
        if (StrUtil.isNotBlank(file.getType())) {
            reqVO.setSourceMimeType(file.getType());
        }
        if (file.getSize() != null) {
            reqVO.setSourceSize(file.getSize());
        }
        if (StrUtil.isNotBlank(reqVO.getSourceMimeType())
                && !SUPPORTED_IMAGE_MIME_TYPES.contains(reqVO.getSourceMimeType().toLowerCase())) {
            throw exception(MEDIA_VIDEO_SOURCE_INVALID, "仅支持 jpg/png/webp 图片");
        }
        if (reqVO.getSourceSize() != null && videoMaxSourceBytes != null
                && videoMaxSourceBytes > 0 && reqVO.getSourceSize() > videoMaxSourceBytes) {
            throw exception(MEDIA_VIDEO_SOURCE_INVALID, "源图片大小超过限制");
        }
    }

    private FileRespDTO getSourceFile(String sourceImageUrl) {
        try {
            return fileApi.getFileByUrl(sourceImageUrl).getCheckedData();
        } catch (Exception e) {
            throw exception(MEDIA_VIDEO_SOURCE_INVALID, "源图片文件不存在");
        }
    }

    private boolean isProjectFileUrl(String sourceImageUrl) {
        try {
            String path = URI.create(sourceImageUrl.trim()).getPath();
            return StrUtil.contains(path, "/infra/file/") && StrUtil.contains(path, "/get/");
        } catch (Exception e) {
            return false;
        }
    }

    private AiModelDO validateTtsModel(Long modelId, AiVoiceProfileSourceTypeEnum voiceMode) {
        AiModelDO model = modelId != null ? modelService.validateModel(modelId)
                : modelService.getRequiredDefaultModel(AiModelTypeEnum.VOICE.getType());
        if (!AiModelTypeEnum.VOICE.getType().equals(model.getType())) {
            throw exception(MODEL_USE_TYPE_ERROR);
        }
        if (!AiPlatformEnum.XIAOMI_MIMO.getPlatform().equals(model.getPlatform())) {
            throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "请配置 XiaomiMiMo 语音模型");
        }
        boolean matches = switch (voiceMode) {
            case BUILTIN -> XiaomiMiMoTtsProviderAdapter.MODEL_TTS.equals(model.getModel());
            case CLONE -> XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_CLONE.equals(model.getModel());
            case DESIGN -> XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_DESIGN.equals(model.getModel());
        };
        if (!matches) {
            throw exception(MEDIA_PROVIDER_CONFIG_MISSING,
                    StrUtil.format("{} 模式需要配置对应的小米语音模型，当前模型({})不匹配", voiceMode.getName(), model.getModel()));
        }
        return model;
    }

    private AiModelDO validateVideoModel(Long modelId) {
        AiModelDO model = modelId != null ? modelService.validateModel(modelId, AiModelTypeEnum.VIDEO)
                : modelService.getRequiredDefaultModel(AiModelTypeEnum.VIDEO.getType());
        if (!AiModelTypeEnum.VIDEO.getType().equals(model.getType())) {
            throw exception(MODEL_USE_TYPE_ERROR);
        }
        if (!AiPlatformEnum.SILICON_FLOW.getPlatform().equals(model.getPlatform())
                && !AiPlatformEnum.PYTHON_WORKER.getPlatform().equals(model.getPlatform())) {
            throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "请配置 SiliconFlow 或 PythonWorker 视频模型");
        }
        if (!AiModelCapabilities.infer(model).getCapabilities()
                .contains(AiModelCapabilities.CAPABILITY_IMAGE_TO_VIDEO)) {
            throw exception(MEDIA_VIDEO_MODEL_CAPABILITY_UNSUPPORTED);
        }
        return model;
    }

    private AiMediaTtsCreateReqVO applyVoiceProfile(Long userId, AiMediaTtsCreateReqVO reqVO) {
        if (reqVO.getVoiceProfileId() == null) {
            if (StrUtil.isBlank(reqVO.getVoiceMode())) {
                reqVO.setVoiceMode(AiVoiceProfileSourceTypeEnum.BUILTIN.getType());
            }
            return reqVO;
        }
        AiVoiceProfileDO profile = voiceProfileMapper.selectById(reqVO.getVoiceProfileId());
        if (profile == null || ObjUtil.notEqual(profile.getUserId(), userId)) {
            throw exception(VOICE_PROFILE_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(profile.getStatus())) {
            throw exception(VOICE_PROFILE_DISABLE);
        }
        return reqVO
                .setModelId(profile.getModelId())
                .setVoiceMode(profile.getSourceType())
                .setVoice(profile.getBuiltinVoice())
                .setDesignPrompt(profile.getDesignPrompt())
                .setSampleFileUrl(profile.getSampleFileUrl())
                .setSampleMimeType(profile.getSampleMimeType())
                .setSampleSize(profile.getSampleSize())
                .setConsentConfirmed(profile.getConsentConfirmed());
    }

    private Map<String, Object> buildTtsInput(AiMediaTtsCreateReqVO reqVO) {
        Map<String, Object> input = BeanUtils.toBean(reqVO, LinkedHashMap.class);
        input.remove("modelId");
        input.remove("sampleSize");
        return input;
    }

    private Map<String, Object> buildImageToVideoInput(AiMediaImageToVideoCreateReqVO reqVO) {
        Map<String, Object> input = BeanUtils.toBean(reqVO, LinkedHashMap.class);
        input.remove("modelId");
        input.put("taskKind", "IMAGE_TO_VIDEO");
        input.put("sourceAsset", buildSourceImageAsset(reqVO));
        return input;
    }

    private Map<String, Object> buildSourceImageAsset(AiMediaImageToVideoCreateReqVO reqVO) {
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("assetType", "SOURCE_IMAGE");
        asset.put("url", reqVO.getSourceImageUrl());
        asset.put("mimeType", reqVO.getSourceMimeType());
        asset.put("size", reqVO.getSourceSize());
        return asset;
    }

    private Map<String, Object> buildResultVideoAsset(AiMediaTaskDO task, String fileUrl, String mimeType, long size) {
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("assetType", "RESULT_VIDEO");
        asset.put("fileId", fileUrl);
        asset.put("url", fileUrl);
        asset.put("mimeType", mimeType);
        asset.put("size", size);
        asset.put("duration", toInteger(task.getInputJson() == null ? null : task.getInputJson().get("duration")));
        putImageSize(asset, task.getInputJson() == null ? null : task.getInputJson().get("imageSize"));
        asset.put("expireTime", calculateAssetExpireTime(LocalDateTime.now()));
        asset.put("cleanupStatus", "ACTIVE");
        return asset;
    }

    private List<AiMediaAssetRespVO> buildAssetList(List<AiMediaTaskDO> tasks, AiMediaAssetPageReqVO pageReqVO) {
        List<AiMediaAssetRespVO> assets = new ArrayList<>();
        for (AiMediaTaskDO task : tasks) {
            for (AiMediaAssetRespVO asset : buildTaskAssets(task)) {
                if (matchesAssetQuery(asset, pageReqVO)) {
                    assets.add(asset);
                }
            }
        }
        return assets;
    }

    private PageResult<AiMediaAssetRespVO> buildAssetPageResult(List<AiMediaAssetRespVO> assets,
                                                               AiMediaAssetPageReqVO pageReqVO) {
        int total = assets.size();
        int pageNo = pageReqVO.getPageNo() == null ? 1 : pageReqVO.getPageNo();
        int pageSize = pageReqVO.getPageSize() == null ? 10 : pageReqVO.getPageSize();
        int fromIndex = Math.min(Math.max(pageNo - 1, 0) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PageResult<>(assets.subList(fromIndex, toIndex), (long) total);
    }

    private List<AiMediaAssetRespVO> buildTaskAssets(AiMediaTaskDO task) {
        List<AiMediaAssetRespVO> assets = new ArrayList<>();
        if (task.getInputJson() != null) {
            addAssetValue(assets, task, task.getInputJson().get("sourceAsset"), "SOURCE_FILE");
        }
        if (assets.isEmpty() && task.getSourceFileIds() != null) {
            for (String sourceFileId : task.getSourceFileIds()) {
                addAssetValue(assets, task, Map.of("url", sourceFileId), "SOURCE_FILE");
            }
        }
        if (task.getOutputJson() != null) {
            addAssetValue(assets, task, task.getOutputJson().get("resultAsset"), defaultResultAssetType(task));
            if (task.getOutputJson().get("resultAsset") == null && task.getOutputJson().get("url") != null) {
                addAssetValue(assets, task, task.getOutputJson(), defaultResultAssetType(task));
            }
        }
        if (task.getResultFileIds() != null && assets.stream().noneMatch(asset -> isResultAsset(asset.getAssetType()))) {
            for (String resultFileId : task.getResultFileIds()) {
                addAssetValue(assets, task, Map.of("url", resultFileId, "fileId", resultFileId), defaultResultAssetType(task));
            }
        }
        return assets;
    }

    private void addAssetValue(List<AiMediaAssetRespVO> assets, AiMediaTaskDO task, Object value,
                               String defaultAssetType) {
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                addAssetValue(assets, task, item, defaultAssetType);
            }
            return;
        }
        Map<String, Object> assetMap = toStringObjectMap(value);
        if (assetMap == null) {
            return;
        }
        AiMediaAssetRespVO asset = new AiMediaAssetRespVO()
                .setTaskId(task.getId())
                .setUserId(task.getUserId())
                .setTenantId(task.getTenantId())
                .setTaskType(task.getTaskType())
                .setStatus(task.getStatus())
                .setPlatform(task.getPlatform())
                .setModel(task.getModel())
                .setAssetType(StrUtil.blankToDefault(toString(assetMap.get("assetType")), defaultAssetType))
                .setFileId(StrUtil.blankToDefault(toString(assetMap.get("fileId")), toString(assetMap.get("url"))))
                .setUrl(toString(assetMap.get("url")))
                .setMimeType(toString(assetMap.get("mimeType")))
                .setSize(toLong(assetMap.get("size")))
                .setDuration(toInteger(assetMap.get("duration")))
                .setWidth(toInteger(assetMap.get("width")))
                .setHeight(toInteger(assetMap.get("height")))
                .setCreateTime(task.getCreateTime())
                .setFinishTime(task.getFinishTime());
        if (asset.getWidth() == null || asset.getHeight() == null) {
            applyImageSize(asset, task.getInputJson() == null ? null : task.getInputJson().get("imageSize"));
        }
        asset.setExpireTime(resolveExpireTime(task, asset, assetMap));
        asset.setCleanupCandidate(isCleanupCandidate(task, asset));
        asset.setCleanupStatus(resolveCleanupStatus(asset));
        assets.add(asset);
    }

    private Map<String, Object> toStringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(Objects.toString(key), item));
        return result;
    }

    private boolean matchesAssetQuery(AiMediaAssetRespVO asset, AiMediaAssetPageReqVO pageReqVO) {
        return (StrUtil.isBlank(pageReqVO.getAssetType()) || pageReqVO.getAssetType().equals(asset.getAssetType()))
                && (pageReqVO.getCleanupCandidate() == null
                || pageReqVO.getCleanupCandidate().equals(asset.getCleanupCandidate()));
    }

    private String defaultResultAssetType(AiMediaTaskDO task) {
        if (AiMediaTaskTypeEnum.TTS.getType().equals(task.getTaskType())) {
            return "RESULT_AUDIO";
        }
        if (AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType().equals(task.getTaskType())
                || AiMediaTaskTypeEnum.TEXT_TO_VIDEO.getType().equals(task.getTaskType())) {
            return "RESULT_VIDEO";
        }
        return "RESULT_FILE";
    }

    private boolean isResultAsset(String assetType) {
        return StrUtil.startWith(assetType, "RESULT_");
    }

    private LocalDateTime resolveExpireTime(AiMediaTaskDO task, AiMediaAssetRespVO asset, Map<String, Object> assetMap) {
        LocalDateTime expireTime = toLocalDateTime(assetMap.get("expireTime"));
        if (expireTime != null || !isResultAsset(asset.getAssetType())) {
            return expireTime;
        }
        LocalDateTime baseTime = task.getFinishTime() == null ? task.getCreateTime() : task.getFinishTime();
        return calculateAssetExpireTime(baseTime);
    }

    private LocalDateTime calculateAssetExpireTime(LocalDateTime baseTime) {
        if (baseTime == null || assetRetentionDays == null || assetRetentionDays <= 0) {
            return null;
        }
        return baseTime.plusDays(assetRetentionDays);
    }

    private boolean isCleanupCandidate(AiMediaTaskDO task, AiMediaAssetRespVO asset) {
        return isResultAsset(asset.getAssetType())
                && AiMediaTaskStatusEnum.isTerminal(task.getStatus())
                && asset.getExpireTime() != null
                && !asset.getExpireTime().isAfter(LocalDateTime.now());
    }

    private String resolveCleanupStatus(AiMediaAssetRespVO asset) {
        if (!isResultAsset(asset.getAssetType())) {
            return "RETAINED_SOURCE";
        }
        return Boolean.TRUE.equals(asset.getCleanupCandidate()) ? "EXPIRED_CANDIDATE" : "ACTIVE";
    }

    private void putImageSize(Map<String, Object> asset, Object imageSize) {
        Integer[] widthHeight = parseImageSize(imageSize);
        if (widthHeight == null) {
            return;
        }
        asset.put("width", widthHeight[0]);
        asset.put("height", widthHeight[1]);
    }

    private void applyImageSize(AiMediaAssetRespVO asset, Object imageSize) {
        Integer[] widthHeight = parseImageSize(imageSize);
        if (widthHeight == null) {
            return;
        }
        if (asset.getWidth() == null) {
            asset.setWidth(widthHeight[0]);
        }
        if (asset.getHeight() == null) {
            asset.setHeight(widthHeight[1]);
        }
    }

    private Integer[] parseImageSize(Object imageSize) {
        if (imageSize == null) {
            return null;
        }
        List<String> parts = StrUtil.split(Objects.toString(imageSize), 'x');
        if (parts.size() != 2) {
            return null;
        }
        try {
            return new Integer[]{Integer.valueOf(parts.get(0)), Integer.valueOf(parts.get(1))};
        } catch (Exception e) {
            return null;
        }
    }

    private String toString(Object value) {
        return value == null ? null : Objects.toString(value);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(Objects.toString(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(Objects.toString(value));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        try {
            return value == null ? null : LocalDateTime.parse(Objects.toString(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildUsageRecord(AiMediaTaskDO task, Integer terminalStatus, String errorCode,
                                                 Long resultSize, String resultMimeType) {
        Map<String, Object> usageRecord = new LinkedHashMap<>();
        usageRecord.put("recordedTime", LocalDateTime.now().toString());
        usageRecord.put("terminalStatus", terminalStatus);
        usageRecord.put("errorCode", errorCode);
        if (task == null) {
            return usageRecord;
        }
        usageRecord.put("taskId", task.getId());
        usageRecord.put("taskType", task.getTaskType());
        usageRecord.put("tenantId", task.getTenantId());
        usageRecord.put("userId", task.getUserId());
        usageRecord.put("provider", task.getPlatform());
        usageRecord.put("model", task.getModel());
        usageRecord.put("modelId", task.getModelId());
        usageRecord.put("sourceSize", resolveSourceSize(task));
        usageRecord.put("resultSize", resultSize == null ? resolveResultSize(task) : resultSize);
        usageRecord.put("resultMimeType", StrUtil.blankToDefault(resultMimeType, resolveResultMimeType(task)));
        usageRecord.put("duration", resolveDuration(task));
        usageRecord.put("submittedTime", task.getOutputJson() == null ? null : task.getOutputJson().get("submittedTime"));
        usageRecord.put("elapsedMillis", calculateElapsedMillis(task));
        usageRecord.put("estimatedCost", null);
        usageRecord.put("costCurrency", null);
        return usageRecord;
    }

    private Long resolveSourceSize(AiMediaTaskDO task) {
        Object sourceAsset = task.getInputJson() == null ? null : task.getInputJson().get("sourceAsset");
        Map<String, Object> sourceAssetMap = toStringObjectMap(sourceAsset);
        return sourceAssetMap == null ? null : toLong(sourceAssetMap.get("size"));
    }

    private Long resolveResultSize(AiMediaTaskDO task) {
        Object resultAsset = task.getOutputJson() == null ? null : task.getOutputJson().get("resultAsset");
        Map<String, Object> resultAssetMap = toStringObjectMap(resultAsset);
        return resultAssetMap == null ? null : toLong(resultAssetMap.get("size"));
    }

    private String resolveResultMimeType(AiMediaTaskDO task) {
        Object resultAsset = task.getOutputJson() == null ? null : task.getOutputJson().get("resultAsset");
        Map<String, Object> resultAssetMap = toStringObjectMap(resultAsset);
        if (resultAssetMap != null) {
            return toString(resultAssetMap.get("mimeType"));
        }
        return task.getOutputJson() == null ? null : toString(task.getOutputJson().get("mimeType"));
    }

    private Integer resolveDuration(AiMediaTaskDO task) {
        if (task.getInputJson() == null) {
            return null;
        }
        return toInteger(task.getInputJson().get("duration"));
    }

    private Long calculateElapsedMillis(AiMediaTaskDO task) {
        if (task.getCreateTime() == null) {
            return null;
        }
        return java.time.Duration.between(task.getCreateTime(), LocalDateTime.now()).toMillis();
    }

    private Map<String, Object> mergeCancelOutput(AiMediaTaskDO task, boolean providerCancelled) {
        Map<String, Object> outputJson = mergeOutputJson(task.getOutputJson());
        outputJson.put("cancelRequested", true);
        outputJson.put("cancelRequestedTime", LocalDateTime.now().toString());
        outputJson.put("providerCancelled", providerCancelled);
        outputJson.putIfAbsent("retryCount", 0);
        return outputJson;
    }

    private void updateTaskCancelled(AiMediaTaskDO task, String stage, Map<String, Object> outputJson) {
        mediaTaskMapper.updateById(new AiMediaTaskDO()
                .setId(task.getId())
                .setStatus(AiMediaTaskStatusEnum.CANCELLED.getStatus())
                .setProgress(100)
                .setStage(stage)
                .setOutputJson(outputJson)
                .setFinishTime(LocalDateTime.now()));
    }

    private boolean isCancelRequested(AiMediaTaskDO task) {
        return task != null && task.getOutputJson() != null
                && Boolean.TRUE.equals(task.getOutputJson().get("cancelRequested"));
    }

    private int getRetryCount(AiMediaTaskDO task) {
        if (task == null || task.getOutputJson() == null) {
            return 0;
        }
        Object retryCount = task.getOutputJson().get("retryCount");
        if (retryCount instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(Objects.toString(retryCount, "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> mergeOutputJson(Map<String, Object> outputJson) {
        if (outputJson == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(outputJson);
    }

    private List<String> buildTtsSourceFileIds(AiMediaTtsCreateReqVO reqVO, AiVoiceProfileSourceTypeEnum voiceMode) {
        if (voiceMode != AiVoiceProfileSourceTypeEnum.CLONE || StrUtil.isBlank(reqVO.getSampleFileUrl())) {
            return null;
        }
        return List.of(reqVO.getSampleFileUrl());
    }

    private AiVoiceProfileSourceTypeEnum parseVoiceMode(String voiceMode) {
        String type = StrUtil.blankToDefault(voiceMode, AiVoiceProfileSourceTypeEnum.BUILTIN.getType());
        for (AiVoiceProfileSourceTypeEnum value : AiVoiceProfileSourceTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "未知 TTS 音色模式");
    }

    private AiMediaTaskDO validateTaskExists(Long id) {
        AiMediaTaskDO task = mediaTaskMapper.selectById(id);
        if (task == null) {
            throw exception(MEDIA_TASK_NOT_EXISTS);
        }
        return task;
    }

}
