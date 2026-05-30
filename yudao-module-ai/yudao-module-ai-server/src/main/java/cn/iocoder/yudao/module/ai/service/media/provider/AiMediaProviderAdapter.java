package cn.iocoder.yudao.module.ai.service.media.provider;

import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;

public interface AiMediaProviderAdapter {

    boolean supports(Integer taskType, String platform, String model);

    AiMediaProviderResult execute(AiMediaTaskDO task);

    default AiMediaProviderSubmitResult submit(AiMediaTaskDO task) {
        throw new UnsupportedOperationException("async submit not supported");
    }

    default AiMediaProviderQueryResult query(AiMediaTaskDO task) {
        throw new UnsupportedOperationException("async query not supported");
    }

    default void cancel(AiMediaTaskDO task) {
        throw new UnsupportedOperationException("cancel not supported");
    }

    default AiMediaProviderQueryResult handleCallback(AiMediaTaskDO task, Object payload) {
        throw new UnsupportedOperationException("callback not supported");
    }

    default boolean verifyCallbackToken(String token) {
        return false;
    }

}
