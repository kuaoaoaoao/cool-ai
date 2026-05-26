package cn.iocoder.yudao.module.ai.service.media.provider;

import lombok.Getter;

@Getter
public class AiMediaProviderException extends RuntimeException {

    private final String code;

    public AiMediaProviderException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AiMediaProviderException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}
