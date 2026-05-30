package cn.iocoder.yudao.module.ai.service.media.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMediaProviderSubmitResult {

    private String providerTaskId;

    private Integer status;

    private Integer progress;

    private String stage;

    private Map<String, Object> rawOutput;

}
