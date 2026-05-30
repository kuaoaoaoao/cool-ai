package cn.iocoder.yudao.module.ai.service.media.sensitive;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class AiSensitiveMediaValidationResult {

    private String capability;

    private String permission;

    private Boolean watermarkEnabled;

    private String traceId;

    private Map<String, Object> auditPayload;

    private Map<String, Object> workerPayload;

}
