package cn.iocoder.yudao.module.ai.service.media;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfilePageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfilePreviewReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfileSaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiVoiceProfileDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiVoiceProfileMapper;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.media.provider.xiaomi.XiaomiMiMoTtsProviderAdapter;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.*;

/**
 * AI 音色库 Service 实现。
 *
 * @author codex
 */
@Service
@Validated
public class AiVoiceProfileServiceImpl implements AiVoiceProfileService {

    public static final long MAX_CLONE_SAMPLE_FILE_BYTES = 7_500_000L;

    @Resource
    private AiVoiceProfileMapper voiceProfileMapper;
    @Resource
    private AiModelService modelService;
    @Resource
    private AiMediaTaskService mediaTaskService;

    @Override
    public Long createVoiceProfile(Long userId, AiVoiceProfileSaveReqVO reqVO) {
        AiModelDO model = validateAndGetModel(reqVO);
        AiVoiceProfileDO profile = BeanUtils.toBean(reqVO, AiVoiceProfileDO.class)
                .setUserId(userId)
                .setPlatform(model.getPlatform())
                .setModelId(model.getId())
                .setModel(model.getModel());
        clearUnusedSourceFields(profile, parseSourceType(reqVO.getSourceType()));
        voiceProfileMapper.insert(profile);
        return profile.getId();
    }

    @Override
    public void updateVoiceProfile(Long userId, AiVoiceProfileSaveReqVO reqVO) {
        AiVoiceProfileDO oldProfile = validateUserVoiceProfile(userId, reqVO.getId());
        AiModelDO model = validateAndGetModel(reqVO);
        AiVoiceProfileDO updateObj = BeanUtils.toBean(reqVO, AiVoiceProfileDO.class)
                .setId(oldProfile.getId())
                .setUserId(userId)
                .setPlatform(model.getPlatform())
                .setModelId(model.getId())
                .setModel(model.getModel());
        updateVoiceProfileWithClearedFields(updateObj, parseSourceType(reqVO.getSourceType()));
    }

    @Override
    public void deleteVoiceProfile(Long userId, Long id) {
        validateUserVoiceProfile(userId, id);
        voiceProfileMapper.deleteById(id);
    }

    @Override
    public AiVoiceProfileDO getVoiceProfile(Long userId, Long id) {
        AiVoiceProfileDO profile = voiceProfileMapper.selectById(id);
        if (profile == null || ObjUtil.notEqual(profile.getUserId(), userId)) {
            return null;
        }
        return profile;
    }

    @Override
    public AiVoiceProfileDO validateUserVoiceProfile(Long userId, Long id) {
        AiVoiceProfileDO profile = voiceProfileMapper.selectById(id);
        if (profile == null) {
            throw exception(VOICE_PROFILE_NOT_EXISTS);
        }
        if (ObjUtil.notEqual(profile.getUserId(), userId)) {
            throw exception(VOICE_PROFILE_FORBIDDEN);
        }
        return profile;
    }

    @Override
    public PageResult<AiVoiceProfileDO> getVoiceProfilePage(Long userId, AiVoiceProfilePageReqVO pageReqVO) {
        return voiceProfileMapper.selectPage(pageReqVO, userId);
    }

    @Override
    public List<AiVoiceProfileDO> getVoiceProfileSimpleList(Long userId, Integer status) {
        return voiceProfileMapper.selectListByUserIdAndStatus(userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long previewVoiceProfile(Long userId, AiVoiceProfilePreviewReqVO reqVO) {
        AiVoiceProfileDO profile = validateUserVoiceProfile(userId, reqVO.getId());
        if (CommonStatusEnum.isDisable(profile.getStatus())) {
            throw exception(VOICE_PROFILE_DISABLE);
        }
        AiMediaTtsCreateReqVO ttsReqVO = new AiMediaTtsCreateReqVO()
                .setVoiceProfileId(profile.getId())
                .setText(reqVO.getText())
                .setFormat(StrUtil.blankToDefault(reqVO.getFormat(), "wav"));
        Long taskId = mediaTaskService.createTtsTask(userId, ttsReqVO);
        voiceProfileMapper.updateById(new AiVoiceProfileDO().setId(profile.getId()).setPreviewTaskId(taskId));
        return taskId;
    }

    @Override
    public AiMediaTtsCreateReqVO applyVoiceProfile(Long userId, AiMediaTtsCreateReqVO reqVO) {
        if (reqVO.getVoiceProfileId() == null) {
            return reqVO;
        }
        AiVoiceProfileDO profile = validateUserVoiceProfile(userId, reqVO.getVoiceProfileId());
        if (CommonStatusEnum.isDisable(profile.getStatus())) {
            throw exception(VOICE_PROFILE_DISABLE);
        }
        return reqVO
                .setModelId(profile.getModelId())
                .setVoiceMode(profile.getSourceType())
                .setVoice(profile.getBuiltinVoice())
                .setDesignPrompt(profile.getDesignPrompt())
                .setSampleFileUrl(profile.getSampleFileUrl())
                .setSampleMimeType(profile.getSampleMimeType())
                .setSampleSize(profile.getSampleSize())
                .setConsentConfirmed(profile.getConsentConfirmed());
    }

    private AiModelDO validateAndGetModel(AiVoiceProfileSaveReqVO reqVO) {
        AiVoiceProfileSourceTypeEnum sourceType = parseSourceType(reqVO.getSourceType());
        AiModelDO model = modelService.validateModel(reqVO.getModelId(), AiModelTypeEnum.VOICE);
        if (!AiPlatformEnum.XIAOMI_MIMO.getPlatform().equals(model.getPlatform())) {
            throw exception(VOICE_PROFILE_CONFIG_ERROR, "音色库第一版只支持 XiaomiMiMo 语音模型");
        }
        validateModelMatchesSourceType(sourceType, model.getModel());
        validateSourceConfig(sourceType, reqVO);
        return model;
    }

    private void validateModelMatchesSourceType(AiVoiceProfileSourceTypeEnum sourceType, String model) {
        boolean matches = switch (sourceType) {
            case BUILTIN -> XiaomiMiMoTtsProviderAdapter.MODEL_TTS.equals(model);
            case CLONE -> XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_CLONE.equals(model);
            case DESIGN -> XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_DESIGN.equals(model);
        };
        if (!matches) {
            throw exception(VOICE_PROFILE_CONFIG_ERROR,
                    StrUtil.format("{} 音色需要使用匹配的小米模型，当前模型({})不匹配", sourceType.getName(), model));
        }
    }

    private void validateSourceConfig(AiVoiceProfileSourceTypeEnum sourceType, AiVoiceProfileSaveReqVO reqVO) {
        switch (sourceType) {
            case BUILTIN -> {
                if (StrUtil.isBlank(reqVO.getBuiltinVoice())) {
                    throw exception(VOICE_PROFILE_CONFIG_ERROR, "预置音色不能为空");
                }
            }
            case DESIGN -> {
                if (StrUtil.isBlank(reqVO.getDesignPrompt())) {
                    throw exception(VOICE_PROFILE_CONFIG_ERROR, "文字设计音色描述不能为空");
                }
            }
            case CLONE -> {
                if (!Boolean.TRUE.equals(reqVO.getConsentConfirmed())) {
                    throw exception(VOICE_PROFILE_CONFIG_ERROR, "音色克隆必须确认已获得声音授权");
                }
                if (StrUtil.isBlank(reqVO.getSampleFileUrl())) {
                    throw exception(VOICE_PROFILE_CONFIG_ERROR, "音色克隆样本不能为空");
                }
                if (!isSupportedSampleMimeType(reqVO.getSampleMimeType(), reqVO.getSampleFileUrl())) {
                    throw exception(VOICE_PROFILE_CONFIG_ERROR, "音色克隆样本只支持 mp3/wav");
                }
                if (reqVO.getSampleSize() != null && reqVO.getSampleSize() > MAX_CLONE_SAMPLE_FILE_BYTES) {
                    throw exception(VOICE_PROFILE_CONFIG_ERROR, "样本文件过大，请控制在 7 MB 左右");
                }
            }
        }
    }

    private void clearUnusedSourceFields(AiVoiceProfileDO profile, AiVoiceProfileSourceTypeEnum sourceType) {
        switch (sourceType) {
            case BUILTIN -> profile.setDesignPrompt(null)
                    .setSampleFileUrl(null)
                    .setSampleMimeType(null)
                    .setSampleSize(null)
                    .setConsentConfirmed(null);
            case DESIGN -> profile.setBuiltinVoice(null)
                    .setSampleFileUrl(null)
                    .setSampleMimeType(null)
                    .setSampleSize(null)
                    .setConsentConfirmed(null);
            case CLONE -> profile.setBuiltinVoice(null)
                    .setDesignPrompt(null);
        }
    }

    private void updateVoiceProfileWithClearedFields(AiVoiceProfileDO updateObj,
            AiVoiceProfileSourceTypeEnum sourceType) {
        clearUnusedSourceFields(updateObj, sourceType);
        LambdaUpdateWrapper<AiVoiceProfileDO> updateWrapper = new LambdaUpdateWrapper<AiVoiceProfileDO>()
                .eq(AiVoiceProfileDO::getId, updateObj.getId())
                .set(AiVoiceProfileDO::getUserId, updateObj.getUserId())
                .set(AiVoiceProfileDO::getName, updateObj.getName())
                .set(AiVoiceProfileDO::getSourceType, updateObj.getSourceType())
                .set(AiVoiceProfileDO::getPlatform, updateObj.getPlatform())
                .set(AiVoiceProfileDO::getModelId, updateObj.getModelId())
                .set(AiVoiceProfileDO::getModel, updateObj.getModel())
                .set(AiVoiceProfileDO::getBuiltinVoice, updateObj.getBuiltinVoice())
                .set(AiVoiceProfileDO::getDesignPrompt, updateObj.getDesignPrompt())
                .set(AiVoiceProfileDO::getSampleFileUrl, updateObj.getSampleFileUrl())
                .set(AiVoiceProfileDO::getSampleMimeType, updateObj.getSampleMimeType())
                .set(AiVoiceProfileDO::getSampleSize, updateObj.getSampleSize())
                .set(AiVoiceProfileDO::getConsentConfirmed, updateObj.getConsentConfirmed())
                .set(AiVoiceProfileDO::getStatus, updateObj.getStatus())
                .set(AiVoiceProfileDO::getRemark, updateObj.getRemark());
        voiceProfileMapper.update(null, updateWrapper);
    }

    private static AiVoiceProfileSourceTypeEnum parseSourceType(String sourceType) {
        for (AiVoiceProfileSourceTypeEnum value : AiVoiceProfileSourceTypeEnum.values()) {
            if (value.getType().equals(sourceType)) {
                return value;
            }
        }
        throw exception(VOICE_PROFILE_CONFIG_ERROR, "未知音色来源类型");
    }

    private static boolean isSupportedSampleMimeType(String mimeType, String url) {
        if (StrUtil.equalsAnyIgnoreCase(mimeType, "audio/mpeg", "audio/mp3", "audio/wav", "audio/wave", "audio/x-wav")) {
            return true;
        }
        return StrUtil.endWithAnyIgnoreCase(url, ".mp3", ".wav");
    }

}
