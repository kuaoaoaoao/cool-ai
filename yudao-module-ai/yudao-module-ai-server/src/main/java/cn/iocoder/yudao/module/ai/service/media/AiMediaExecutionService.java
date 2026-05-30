package cn.iocoder.yudao.module.ai.service.media;

import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderQueryResult;

/**
 * AI 媒体任务执行边界。
 */
public interface AiMediaExecutionService {

    void enqueueTask(AiMediaTaskDO task);

    void submitTask(AiMediaTaskDO task);

    void refreshTask(AiMediaTaskDO task);

    void retryTask(Long id, Long userId);

    void cancelTask(Long id, Long userId);

    void completeTask(AiMediaTaskDO task, AiMediaProviderQueryResult queryResult);

}
