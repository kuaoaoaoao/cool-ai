package cn.iocoder.yudao.module.ai.service.media.sensitive;

public interface AiSensitiveMediaControlService {

    AiSensitiveMediaValidationResult validateCreate(AiSensitiveMediaCreateRequest request);

    void auditTaskEvent(String capability, Long userId, Long taskId, String event, boolean success,
                        String rejectReason, Object details);

}
