package cn.iocoder.yudao.module.ai.service.media;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfilePageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfilePreviewReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfileSaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiVoiceProfileDO;

import java.util.List;

/**
 * AI 音色库 Service。
 *
 * @author codex
 */
public interface AiVoiceProfileService {

    Long createVoiceProfile(Long userId, AiVoiceProfileSaveReqVO reqVO);

    void updateVoiceProfile(Long userId, AiVoiceProfileSaveReqVO reqVO);

    void deleteVoiceProfile(Long userId, Long id);

    AiVoiceProfileDO getVoiceProfile(Long userId, Long id);

    AiVoiceProfileDO validateUserVoiceProfile(Long userId, Long id);

    PageResult<AiVoiceProfileDO> getVoiceProfilePage(Long userId, AiVoiceProfilePageReqVO pageReqVO);

    List<AiVoiceProfileDO> getVoiceProfileSimpleList(Long userId, Integer status);

    Long previewVoiceProfile(Long userId, AiVoiceProfilePreviewReqVO reqVO);

    AiMediaTtsCreateReqVO applyVoiceProfile(Long userId, AiMediaTtsCreateReqVO reqVO);

}
