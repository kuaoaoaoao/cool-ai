package cn.iocoder.yudao.module.ai.service.media;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskCreateReqVO;
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
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapter;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapterResolver;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderException;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderResult;
import cn.iocoder.yudao.module.ai.service.media.provider.xiaomi.XiaomiMiMoTtsProviderAdapter;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class AiMediaTaskServiceImpl implements AiMediaTaskService {

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

    @Value("${yudao.ai.media.tts.enable:true}")
    private Boolean ttsEnable;
    @Value("${yudao.ai.media.tts.user-daily-limit:100}")
    private Integer ttsUserDailyLimit;
    @Value("${yudao.ai.media.tts.quota-lock-strict:true}")
    private Boolean ttsQuotaLockStrict;

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
    public Long createTask(Long userId, AiMediaTaskCreateReqVO reqVO) {
        if (AiMediaTaskTypeEnum.TTS.getType().equals(reqVO.getTaskType())) {
            AiMediaTtsCreateReqVO ttsReqVO = JSONUtil.toBean(JSONUtil.toJsonStr(reqVO.getInputJson()),
                    AiMediaTtsCreateReqVO.class);
            return createTtsTask(userId, ttsReqVO);
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
    public PageResult<AiMediaTaskDO> getTaskPage(AiMediaTaskPageReqVO pageReqVO) {
        return mediaTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AiMediaTaskDO> getUserTaskPage(AiMediaTaskPageReqVO pageReqVO, Long userId) {
        return mediaTaskMapper.selectPageByUser(pageReqVO, userId);
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
        Map<String, Object> outputJson = new LinkedHashMap<>();
        outputJson.put("fileId", fileUrl);
        outputJson.put("url", fileUrl);
        outputJson.put("mimeType", mimeType);
        outputJson.put("providerOutput", providerOutput);
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
        mediaTaskMapper.updateById(new AiMediaTaskDO()
                .setId(id)
                .setStatus(AiMediaTaskStatusEnum.FAILED.getStatus())
                .setProgress(100)
                .setStage("failed")
                .setErrorCode(StrUtil.maxLength(errorCode, 64))
                .setErrorMessage(StrUtil.maxLength(errorMessage, 480))
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

    private void validateTtsEnabled(Long userId) {
        if (!Boolean.TRUE.equals(ttsEnable)) {
            throw exception(MEDIA_TTS_DISABLED);
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
