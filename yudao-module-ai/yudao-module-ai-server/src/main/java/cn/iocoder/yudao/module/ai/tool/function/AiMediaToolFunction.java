package cn.iocoder.yudao.module.ai.tool.function;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaImageToVideoCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaToolAuditDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiMediaToolAuditMapper;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.MEDIA_TASK_TYPE_NOT_ENABLE;

@Component
@Slf4j
public class AiMediaToolFunction {

    private static final String TOOL_CREATE_TTS = "media_create_tts_task";
    private static final String TOOL_CREATE_IMAGE_TO_VIDEO = "media_create_image_to_video_task";
    private static final String TOOL_GET_TASK = "media_get_task_status";

    @Resource
    private AiMediaTaskService mediaTaskService;
    @Resource
    private AiMediaToolAuditMapper mediaToolAuditMapper;
    @Value("${yudao.ai.media.tool.image-to-video-enable:false}")
    private Boolean imageToVideoToolEnable;

    @Tool(name = TOOL_CREATE_TTS, description = "Create a text-to-speech media task. Returns task id and polling hint.")
    public Map<String, Object> createTtsTask(AiMediaTtsToolRequest request) {
        Long userId = getLoginUserId();
        try {
            AiMediaTtsCreateReqVO reqVO = new AiMediaTtsCreateReqVO()
                    .setModelId(request.getModelId())
                    .setText(request.getText())
                    .setStylePrompt(request.getStylePrompt())
                    .setVoice(request.getVoice())
                    .setFormat(request.getFormat());
            Long taskId = mediaTaskService.createTtsTask(userId, reqVO);
            saveAudit(userId, TOOL_CREATE_TTS, taskId, true, null, BeanUtils.toBean(request, Map.class));
            return Map.of("taskId", taskId, "message", "TTS task created. Poll media_get_task_status with this taskId.");
        } catch (ServiceException e) {
            saveAudit(userId, TOOL_CREATE_TTS, null, false, e.getMessage(), BeanUtils.toBean(request, Map.class));
            throw e;
        }
    }

    @Tool(name = TOOL_CREATE_IMAGE_TO_VIDEO, description = "Create an image-to-video media task. Disabled by default; when enabled, returns task id and polling hint.")
    public Map<String, Object> createImageToVideoTask(AiMediaImageToVideoToolRequest request) {
        Long userId = getLoginUserId();
        if (!Boolean.TRUE.equals(imageToVideoToolEnable)) {
            saveAudit(userId, TOOL_CREATE_IMAGE_TO_VIDEO, null, false,
                    "image-to-video chat tool disabled", BeanUtils.toBean(request, Map.class));
            throw exception(MEDIA_TASK_TYPE_NOT_ENABLE, AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType());
        }
        try {
            AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                    .setModelId(request.getModelId())
                    .setSourceImageUrl(request.getSourceImageUrl())
                    .setSourceMimeType(request.getSourceMimeType())
                    .setSourceSize(request.getSourceSize())
                    .setPrompt(request.getPrompt())
                    .setNegativePrompt(request.getNegativePrompt())
                    .setImageSize(request.getImageSize())
                    .setSeed(request.getSeed())
                    .setDuration(request.getDuration())
                    .setQuality(request.getQuality());
            Long taskId = mediaTaskService.createImageToVideoTask(userId, reqVO);
            saveAudit(userId, TOOL_CREATE_IMAGE_TO_VIDEO, taskId, true, null, BeanUtils.toBean(request, Map.class));
            return Map.of("taskId", taskId, "taskType", AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(),
                    "message", "Image-to-video task created. Poll media_get_task_status with this taskId.");
        } catch (ServiceException e) {
            saveAudit(userId, TOOL_CREATE_IMAGE_TO_VIDEO, null, false, e.getMessage(), BeanUtils.toBean(request, Map.class));
            throw e;
        }
    }

    @Tool(name = TOOL_GET_TASK, description = "Get an AI media task status by task id.")
    public AiMediaTaskRespVO getTaskStatus(Long taskId) {
        Long userId = getLoginUserId();
        AiMediaTaskDO task = mediaTaskService.getUserTask(taskId, userId);
        saveAudit(userId, TOOL_GET_TASK, taskId, true, null, Map.of("taskId", taskId));
        return buildTaskRespVO(task);
    }

    private AiMediaTaskRespVO buildTaskRespVO(AiMediaTaskDO task) {
        AiMediaTaskRespVO respVO = BeanUtils.toBean(task, AiMediaTaskRespVO.class);
        Map<String, Object> inputJson = task.getInputJson();
        Map<String, Object> outputJson = task.getOutputJson();
        if (inputJson != null) {
            respVO.setSourceAssets(inputJson.get("sourceAsset"));
        }
        if (outputJson != null) {
            respVO.setExecutionMode(toString(outputJson.get("executionMode")));
            respVO.setRetryCount(toInteger(outputJson.get("retryCount")));
            respVO.setCancelRequested(Boolean.TRUE.equals(outputJson.get("cancelRequested")));
            respVO.setSubmittedTime(toString(outputJson.get("submittedTime")));
            respVO.setCallbackStatus(toString(outputJson.get("callbackStatus")));
            respVO.setResultAssets(outputJson.get("resultAsset"));
        }
        return respVO;
    }

    private String toString(Object value) {
        return value == null ? null : Objects.toString(value);
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

    private void saveAudit(Long userId, String toolName, Long taskId, Boolean success,
                           String rejectReason, Map<String, Object> requestJson) {
        try {
            mediaToolAuditMapper.insert(new AiMediaToolAuditDO()
                    .setUserId(userId)
                    .setToolName(toolName)
                    .setTaskId(taskId)
                    .setSuccess(success)
                    .setRejectReason(rejectReason)
                    .setRequestJson(requestJson));
        } catch (Exception e) {
            log.warn("[saveAudit][toolName({}) taskId({}) 审计记录失败]", toolName, taskId, e);
        }
    }

    @Data
    public static class AiMediaTtsToolRequest {

        private Long modelId;

        private String text;

        private String stylePrompt;

        private String voice;

        private String format;

    }

    @Data
    public static class AiMediaImageToVideoToolRequest {

        private Long modelId;

        private String sourceImageUrl;

        private String sourceMimeType;

        private Long sourceSize;

        private String prompt;

        private String negativePrompt;

        private String imageSize;

        private Long seed;

        private Integer duration;

        private String quality;

    }

}
