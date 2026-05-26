package cn.iocoder.yudao.module.ai.service.media;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiMediaTaskMapper;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private AiModelService modelService;
    @Resource
    private AiMediaProviderAdapterResolver providerAdapterResolver;
    @Resource
    private FileApi fileApi;

    @Value("${yudao.ai.media.tts.enable:true}")
    private Boolean ttsEnable;
    @Value("${yudao.ai.media.tts.user-daily-limit:100}")
    private Integer ttsUserDailyLimit;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTtsTask(Long userId, AiMediaTtsCreateReqVO reqVO) {
        validateTtsEnabled(userId);
        AiModelDO model = validateTtsModel(reqVO.getModelId());

        AiMediaTaskDO task = new AiMediaTaskDO()
                .setUserId(userId)
                .setTaskType(AiMediaTaskTypeEnum.TTS.getType())
                .setPlatform(model.getPlatform())
                .setModelId(model.getId())
                .setModel(model.getModel())
                .setStatus(AiMediaTaskStatusEnum.CREATED.getStatus())
                .setProgress(0)
                .setStage("created")
                .setInputJson(buildTtsInput(reqVO));
        mediaTaskMapper.insert(task);

        executeTtsTask(task);
        return task.getId();
    }

    @Override
    public Long createTask(Long userId, AiMediaTaskCreateReqVO reqVO) {
        if (!AiMediaTaskTypeEnum.TTS.getType().equals(reqVO.getTaskType())) {
            throw exception(MEDIA_TASK_TYPE_NOT_ENABLE, reqVO.getTaskType());
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
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage)
                .setFinishTime(LocalDateTime.now()));
    }

    private void executeTtsTask(AiMediaTaskDO task) {
        updateTaskRunning(task.getId(), "calling_provider");
        task.setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus()).setProgress(50).setStage("calling_provider");
        try {
            AiMediaProviderAdapter adapter = providerAdapterResolver.resolve(task);
            if (adapter == null) {
                throw exception(MEDIA_PROVIDER_NOT_EXISTS);
            }
            AiMediaProviderResult providerResult = adapter.execute(task);
            String fileUrl = fileApi.createFile(providerResult.getContent(), providerResult.getFileName(),
                    "ai-media/tts", providerResult.getMimeType());
            updateTaskSucceeded(task.getId(), "done", fileUrl, providerResult.getMimeType(), providerResult.getRawOutput());
        } catch (AiMediaProviderException e) {
            log.warn("[executeTtsTask][taskId({}) Provider 调用失败：{}]", task.getId(), e.getMessage(), e);
            updateTaskFailed(task.getId(), e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("[executeTtsTask][taskId({}) TTS 执行失败]", task.getId(), e);
            updateTaskFailed(task.getId(), "EXECUTE_FAIL", e.getMessage());
        }
    }

    private void validateTtsEnabled(Long userId) {
        if (!Boolean.TRUE.equals(ttsEnable)) {
            throw exception(MEDIA_TTS_DISABLED);
        }
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

    private AiModelDO validateTtsModel(Long modelId) {
        AiModelDO model = modelId != null ? modelService.validateModel(modelId)
                : modelService.getRequiredDefaultModel(AiModelTypeEnum.VOICE.getType());
        if (!AiModelTypeEnum.VOICE.getType().equals(model.getType())) {
            throw exception(MODEL_USE_TYPE_ERROR);
        }
        if (!AiPlatformEnum.XIAOMI_MIMO.getPlatform().equals(model.getPlatform())
                || !XiaomiMiMoTtsProviderAdapter.MODEL.equals(model.getModel())) {
            throw exception(MEDIA_PROVIDER_CONFIG_MISSING, "请配置 XiaomiMiMo / mimo-v2.5-tts 语音模型");
        }
        return model;
    }

    private Map<String, Object> buildTtsInput(AiMediaTtsCreateReqVO reqVO) {
        Map<String, Object> input = BeanUtils.toBean(reqVO, LinkedHashMap.class);
        input.remove("modelId");
        return input;
    }

    private AiMediaTaskDO validateTaskExists(Long id) {
        AiMediaTaskDO task = mediaTaskMapper.selectById(id);
        if (task == null) {
            throw exception(MEDIA_TASK_NOT_EXISTS);
        }
        return task;
    }

}
