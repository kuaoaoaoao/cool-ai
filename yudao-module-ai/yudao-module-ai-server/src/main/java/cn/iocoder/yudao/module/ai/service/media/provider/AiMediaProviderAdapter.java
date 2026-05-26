package cn.iocoder.yudao.module.ai.service.media.provider;

import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;

public interface AiMediaProviderAdapter {

    boolean supports(Integer taskType, String platform, String model);

    AiMediaProviderResult execute(AiMediaTaskDO task);

}
