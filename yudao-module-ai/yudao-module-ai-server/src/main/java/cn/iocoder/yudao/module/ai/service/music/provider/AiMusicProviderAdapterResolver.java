package cn.iocoder.yudao.module.ai.service.music.provider;

import cn.iocoder.yudao.module.ai.controller.admin.music.vo.AiSunoGenerateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.music.AiMusicDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 音乐 Provider 适配器解析器。
 *
 * @author kuao
 */
@Component
public class AiMusicProviderAdapterResolver {

    @Resource
    private List<AiMusicProviderAdapter> adapters;

    public AiMusicProviderAdapter resolveGenerate(AiModelDO model, AiSunoGenerateReqVO reqVO) {
        return adapters.stream()
                .filter(adapter -> adapter.supportsGenerate(model, reqVO))
                .findFirst()
                .orElse(null);
    }

    public AiMusicProviderAdapter resolveSync(AiMusicDO music) {
        return adapters.stream()
                .filter(adapter -> adapter.supportsSync(music))
                .findFirst()
                .orElse(null);
    }

}
