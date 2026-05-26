package cn.iocoder.yudao.module.ai.service.media.provider;

import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiMediaProviderAdapterResolver {

    @Resource
    private List<AiMediaProviderAdapter> adapters;

    public AiMediaProviderAdapter resolve(AiMediaTaskDO task) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(task.getTaskType(), task.getPlatform(), task.getModel()))
                .findFirst()
                .orElse(null);
    }

}
