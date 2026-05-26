package cn.iocoder.yudao.module.ai.service.media.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMediaProviderResult {

    private byte[] content;

    private String fileName;

    private String mimeType;

    private Map<String, Object> rawOutput;

}
