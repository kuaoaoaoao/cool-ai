package cn.iocoder.yudao.module.ai.service.music.provider;

import cn.iocoder.yudao.module.ai.controller.admin.music.vo.AiSunoGenerateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.music.AiMusicDO;

import java.util.List;

/**
 * AI 音乐 Provider 适配器。
 *
 * @author kuao
 */
public interface AiMusicProviderAdapter {

    /**
     * 是否支持本次生成请求。
     */
    boolean supportsGenerate(AiModelDO model, AiSunoGenerateReqVO reqVO);

    /**
     * 是否支持同步该音乐任务。
     */
    boolean supportsSync(AiMusicDO music);

    /**
     * 提交音乐生成任务。
     */
    List<Long> generate(Long userId, AiSunoGenerateReqVO reqVO, AiModelDO model);

    /**
     * 同步音乐任务结果。
     */
    void sync(List<AiMusicDO> musicList);

}
