package cn.iocoder.yudao.module.ai.service.media.sensitive;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class AiSensitiveMediaCreateRequest {

    private String capability;

    private Long userId;

    private String provider;

    private String model;

    private Boolean consentConfirmed;

    private Boolean riskConfirmed;

    private Boolean watermarkEnabled;

    private String traceId;

    private Map<String, String> sourceUrls;

    private Map<String, String> sourceMimeTypes;

}
