package cn.iocoder.yudao.module.ai.service.media.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMediaProviderQueryResult {

    private Integer status;

    private Integer progress;

    private String stage;

    private String resultUrl;

    private String resultMimeType;

    private String errorCode;

    private String errorMessage;

    private Map<String, Object> rawOutput;

}
