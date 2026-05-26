package cn.iocoder.yudao.module.ai.tool.function;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaToolAuditDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiMediaToolAuditMapper;
import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Component
@Slf4j
public class AiMediaToolFunction {

    private static final String TOOL_CREATE_TTS = "media_create_tts_task";
    private static final String TOOL_GET_TASK = "media_get_task_status";

    @Resource
    private AiMediaTaskService mediaTaskService;
    @Resource
    private AiMediaToolAuditMapper mediaToolAuditMapper;

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

    @Tool(name = TOOL_GET_TASK, description = "Get an AI media task status by task id.")
    public AiMediaTaskRespVO getTaskStatus(Long taskId) {
        Long userId = getLoginUserId();
        AiMediaTaskDO task = mediaTaskService.getUserTask(taskId, userId);
        saveAudit(userId, TOOL_GET_TASK, taskId, true, null, Map.of("taskId", taskId));
        return BeanUtils.toBean(task, AiMediaTaskRespVO.class);
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

}
