package cn.iocoder.yudao.module.ai.service.music.provider;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.music.vo.AiSunoGenerateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.music.AiMusicDO;
import cn.iocoder.yudao.module.ai.dal.mysql.music.AiMusicMapper;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import cn.iocoder.yudao.module.ai.enums.music.AiMusicGenerateModeEnum;
import cn.iocoder.yudao.module.ai.enums.music.AiMusicStatusEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import cn.iocoder.yudao.module.ai.service.media.provider.xiaomi.XiaomiMiMoTtsProviderAdapter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.MUSIC_GENERATE_PARAM_ERROR;

/**
 * 小米 MiMo 音乐 Provider 适配器。
 *
 * 当前先复用 TTS 媒体任务实现“歌词/文本演绎为音频”，后续可替换成真正的唱歌模型调用。
 *
 * @author kuao
 */
@Component
public class XiaomiMiMoMusicProviderAdapter implements AiMusicProviderAdapter {

    private static final String TASK_ID_PREFIX = "media:";

    @Resource
    private AiMediaTaskService mediaTaskService;
    @Resource
    private AiMusicMapper musicMapper;

    @Override
    public boolean supportsGenerate(AiModelDO model, AiSunoGenerateReqVO reqVO) {
        return AiPlatformEnum.XIAOMI_MIMO.getPlatform().equals(reqVO.getPlatform());
    }

    @Override
    public boolean supportsSync(AiMusicDO music) {
        return StrUtil.startWith(music.getTaskId(), TASK_ID_PREFIX);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generate(Long userId, AiSunoGenerateReqVO reqVO, AiModelDO model) {
        if (model == null) {
            throw exception(MUSIC_GENERATE_PARAM_ERROR, "XiaomiMiMo 音乐生成必须选择模型");
        }
        if (!XiaomiMiMoTtsProviderAdapter.MODEL_TTS.equals(model.getModel())) {
            throw exception(MUSIC_GENERATE_PARAM_ERROR, "XiaomiMiMo 唱歌模式仅支持 mimo-v2.5-tts 模型");
        }
        Long mediaTaskId = mediaTaskService.createTtsTask(userId, buildTtsCreateReqVO(reqVO, model));
        AiMusicDO music = new AiMusicDO()
                .setUserId(userId)
                .setTitle(StrUtil.blankToDefault(reqVO.getTitle(), "AI 音乐"))
                .setLyric(Objects.equals(AiMusicGenerateModeEnum.LYRIC.getMode(), reqVO.getGenerateMode())
                        ? reqVO.getPrompt() : null)
                .setDescription(Objects.equals(AiMusicGenerateModeEnum.DESCRIPTION.getMode(), reqVO.getGenerateMode())
                        ? reqVO.getPrompt() : reqVO.getStylePrompt())
                .setStatus(AiMusicStatusEnum.IN_PROGRESS.getStatus())
                .setGenerateMode(reqVO.getGenerateMode())
                .setPlatform(reqVO.getPlatform())
                .setModelId(model.getId())
                .setModel(reqVO.getModel())
                .setTags(reqVO.getTags())
                .setPublicStatus(false)
                .setTaskId(TASK_ID_PREFIX + mediaTaskId);
        musicMapper.insert(music);
        return List.of(music.getId());
    }

    @Override
    public void sync(List<AiMusicDO> musicList) {
        if (CollUtil.isEmpty(musicList)) {
            return;
        }
        musicList.forEach(music -> {
            Long mediaTaskId = parseMediaTaskId(music.getTaskId());
            if (mediaTaskId == null) {
                updateFailed(music, "媒体任务编号无效");
                return;
            }
            AiMediaTaskDO task = mediaTaskService.getTask(mediaTaskId);
            if (task == null) {
                updateFailed(music, "媒体任务不存在");
                return;
            }
            if (AiMediaTaskStatusEnum.SUCCEEDED.getStatus().equals(task.getStatus())) {
                musicMapper.updateById(new AiMusicDO().setId(music.getId())
                        .setStatus(AiMusicStatusEnum.SUCCESS.getStatus())
                        .setAudioUrl(getMediaTaskAudioUrl(task))
                        .setErrorMessage(null));
            } else if (AiMediaTaskStatusEnum.FAILED.getStatus().equals(task.getStatus())
                    || AiMediaTaskStatusEnum.CANCELLED.getStatus().equals(task.getStatus())) {
                updateFailed(music, task.getErrorMessage());
            }
        });
    }

    private AiMediaTtsCreateReqVO buildTtsCreateReqVO(AiSunoGenerateReqVO reqVO, AiModelDO model) {
        AiVoiceProfileSourceTypeEnum voiceMode = resolveVoiceMode(reqVO, model);
        AiMediaTtsCreateReqVO ttsReqVO = new AiMediaTtsCreateReqVO()
                .setModelId(model.getId())
                .setVoiceMode(voiceMode.getType())
                .setText(buildSingingText(reqVO.getPrompt()))
                .setStylePrompt(buildTtsStylePrompt(reqVO))
                .setVoice(StrUtil.blankToDefault(reqVO.getVoice(), "mimo_default"))
                .setFormat(StrUtil.blankToDefault(reqVO.getFormat(), "wav"));
        if (voiceMode == AiVoiceProfileSourceTypeEnum.DESIGN) {
            ttsReqVO.setDesignPrompt(StrUtil.blankToDefault(reqVO.getStylePrompt(), "适合演唱的自然音色"));
        }
        return ttsReqVO;
    }

    private String buildSingingText(String text) {
        text = StrUtil.trim(text);
        if (StrUtil.startWithAny(text, "(唱歌)", "（唱歌）", "[唱歌]",
                "(sing)", "（sing）", "[sing]",
                "(singing)", "（singing）", "[singing]")) {
            return text;
        }
        return "(唱歌)" + text;
    }

    private AiVoiceProfileSourceTypeEnum resolveVoiceMode(AiSunoGenerateReqVO reqVO, AiModelDO model) {
        if (StrUtil.equalsAnyIgnoreCase(reqVO.getVoiceMode(), AiVoiceProfileSourceTypeEnum.DESIGN.getType())) {
            return AiVoiceProfileSourceTypeEnum.DESIGN;
        }
        if (StrUtil.endWith(model.getModel(), "-voicedesign")) {
            return AiVoiceProfileSourceTypeEnum.DESIGN;
        }
        return AiVoiceProfileSourceTypeEnum.BUILTIN;
    }

    private String buildTtsStylePrompt(AiSunoGenerateReqVO reqVO) {
        if (StrUtil.isNotBlank(reqVO.getStylePrompt())) {
            return reqVO.getStylePrompt();
        }
        if (CollUtil.isNotEmpty(reqVO.getTags())) {
            return StrUtil.format("请以 {} 风格演唱这段歌词", CollUtil.join(reqVO.getTags(), StrPool.COMMA));
        }
        return "请用自然、有音乐感的方式演绎这段内容";
    }

    private Long parseMediaTaskId(String taskId) {
        if (!StrUtil.startWith(taskId, TASK_ID_PREFIX)) {
            return null;
        }
        try {
            return Long.valueOf(StrUtil.removePrefix(taskId, TASK_ID_PREFIX));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getMediaTaskAudioUrl(AiMediaTaskDO task) {
        if (task.getOutputJson() != null && task.getOutputJson().get("url") != null) {
            return Objects.toString(task.getOutputJson().get("url"));
        }
        return CollUtil.isNotEmpty(task.getResultFileIds()) ? task.getResultFileIds().get(0) : null;
    }

    private void updateFailed(AiMusicDO music, String errorMessage) {
        musicMapper.updateById(new AiMusicDO().setId(music.getId())
                .setStatus(AiMusicStatusEnum.FAIL.getStatus())
                .setErrorMessage(StrUtil.maxLength(StrUtil.blankToDefault(errorMessage, "媒体任务生成失败"), 480)));
    }

}
