package cn.iocoder.yudao.module.ai.service.music.provider;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.controller.admin.music.vo.AiSunoGenerateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.music.AiMusicDO;
import cn.iocoder.yudao.module.ai.dal.mysql.music.AiMusicMapper;
import cn.iocoder.yudao.module.ai.enums.music.AiMusicGenerateModeEnum;
import cn.iocoder.yudao.module.ai.enums.music.AiMusicStatusEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.suno.api.SunoApi;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.MUSIC_GENERATE_PARAM_ERROR;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.MUSIC_GENERATE_RESPONSE_EMPTY;

/**
 * Suno 音乐 Provider 适配器。
 *
 * @author kuao
 */
@Component
@Slf4j
public class SunoMusicProviderAdapter implements AiMusicProviderAdapter {

    private static final long MAX_MUSIC_DOWNLOAD_BYTES = 100 * 1024 * 1024L;
    private static final String TASK_ID_PREFIX = "suno:";

    @Resource
    private AiModelService modelService;
    @Resource
    private AiMusicMapper musicMapper;
    @Resource
    private FileApi fileApi;
    @Resource
    private AiRemoteResourceService remoteResourceService;

    @Override
    public boolean supportsGenerate(AiModelDO model, AiSunoGenerateReqVO reqVO) {
        return AiPlatformEnum.SUNO.getPlatform().equals(reqVO.getPlatform());
    }

    @Override
    public boolean supportsSync(AiMusicDO music) {
        return AiPlatformEnum.SUNO.getPlatform().equals(music.getPlatform());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generate(Long userId, AiSunoGenerateReqVO reqVO, AiModelDO model) {
        SunoApi sunoApi = modelService.getSunoApi(model.getId());
        List<SunoApi.MusicData> musicDataList;
        if (Objects.equals(AiMusicGenerateModeEnum.DESCRIPTION.getMode(), reqVO.getGenerateMode())) {
            SunoApi.MusicGenerateRequest generateRequest = new SunoApi.MusicGenerateRequest(
                    reqVO.getPrompt(), reqVO.getModel(), reqVO.getMakeInstrumental());
            musicDataList = sunoApi.generate(generateRequest);
        } else if (Objects.equals(AiMusicGenerateModeEnum.LYRIC.getMode(), reqVO.getGenerateMode())) {
            SunoApi.MusicGenerateRequest generateRequest = new SunoApi.MusicGenerateRequest(
                    reqVO.getPrompt(), reqVO.getModel(), CollUtil.join(reqVO.getTags(), StrPool.COMMA), reqVO.getTitle());
            musicDataList = sunoApi.customGenerate(generateRequest);
        } else {
            throw exception(MUSIC_GENERATE_PARAM_ERROR, StrUtil.format("未知生成模式({})", reqVO.getGenerateMode()));
        }

        if (CollUtil.isEmpty(musicDataList)) {
            throw exception(MUSIC_GENERATE_RESPONSE_EMPTY);
        }
        List<AiMusicDO> musicList = buildMusicDOList(musicDataList, model);
        musicList.forEach(music -> music.setUserId(userId).setPlatform(reqVO.getPlatform())
                .setGenerateMode(reqVO.getGenerateMode()).setPublicStatus(false));
        musicMapper.insertBatch(musicList);
        return convertList(musicList, AiMusicDO::getId);
    }

    @Override
    public void sync(List<AiMusicDO> musicList) {
        if (CollUtil.isEmpty(musicList)) {
            return;
        }
        Map<Long, List<AiMusicDO>> modelTaskMap = new LinkedHashMap<>();
        musicList.forEach(music -> modelTaskMap
                .computeIfAbsent(parseModelId(music.getTaskId()), key -> new ArrayList<>()).add(music));

        modelTaskMap.forEach((modelId, tasks) -> {
            SunoApi sunoApi = modelId != null ? modelService.getSunoApi(modelId) : modelService.getSunoApi();
            CollUtil.split(tasks, 36).forEach(chunkList -> syncChunk(sunoApi, modelId, chunkList));
        });
    }

    private void syncChunk(SunoApi sunoApi, Long modelId, List<AiMusicDO> chunkList) {
        Map<String, Long> taskIdMap = convertMap(chunkList, music -> parseProviderTaskId(music.getTaskId()),
                AiMusicDO::getId);
        List<SunoApi.MusicData> musicTaskList = sunoApi.getMusicList(new ArrayList<>(taskIdMap.keySet()));
        if (CollUtil.isEmpty(musicTaskList)) {
            log.warn("Suno 任务同步失败, 任务ID: [{}]", taskIdMap.keySet());
            return;
        }
        List<AiMusicDO> updateMusicList = buildMusicDOList(musicTaskList,
                modelId != null ? new AiModelDO().setId(modelId) : null);
        updateMusicList.forEach(music -> music.setId(taskIdMap.get(parseProviderTaskId(music.getTaskId()))));
        musicMapper.updateBatch(updateMusicList);
    }

    private List<AiMusicDO> buildMusicDOList(List<SunoApi.MusicData> musicList, AiModelDO model) {
        return convertList(musicList, musicData -> {
            Integer status = parseMusicStatus(musicData);
            String audioUrl = downloadFile(status, musicData.audioUrl());
            return new AiMusicDO()
                    .setTaskId(buildTaskId(model, musicData.id()))
                    .setModelId(model != null ? model.getId() : null)
                    .setModel(musicData.modelName())
                    .setDescription(musicData.gptDescriptionPrompt())
                    .setAudioUrl(audioUrl)
                    .setVideoUrl(downloadFile(status, musicData.videoUrl()))
                    .setImageUrl(downloadFile(status, musicData.imageUrl()))
                    .setTitle(musicData.title()).setDuration(musicData.duration())
                    .setLyric(musicData.lyric()).setTags(StrUtil.split(musicData.tags(), StrPool.COMMA))
                    .setErrorMessage(buildErrorMessage(status, audioUrl, musicData))
                    .setStatus(status);
        });
    }

    private String buildTaskId(AiModelDO model, String providerTaskId) {
        return model == null || model.getId() == null ? providerTaskId : TASK_ID_PREFIX + model.getId() + ":" + providerTaskId;
    }

    private Long parseModelId(String taskId) {
        if (!StrUtil.startWith(taskId, TASK_ID_PREFIX)) {
            return null;
        }
        String value = StrUtil.subBetween(taskId, TASK_ID_PREFIX, ":");
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseProviderTaskId(String taskId) {
        if (!StrUtil.startWith(taskId, TASK_ID_PREFIX)) {
            return taskId;
        }
        return StrUtil.subAfter(taskId, ":", true);
    }

    private Integer parseMusicStatus(SunoApi.MusicData musicData) {
        if (Objects.equals("complete", musicData.status())) {
            return StrUtil.isBlank(musicData.audioUrl()) ? AiMusicStatusEnum.FAIL.getStatus()
                    : AiMusicStatusEnum.SUCCESS.getStatus();
        }
        if (Objects.equals("error", musicData.status())) {
            return AiMusicStatusEnum.FAIL.getStatus();
        }
        return AiMusicStatusEnum.IN_PROGRESS.getStatus();
    }

    private String buildErrorMessage(Integer status, String audioUrl, SunoApi.MusicData musicData) {
        if (ObjectUtil.notEqual(status, AiMusicStatusEnum.FAIL.getStatus())) {
            return musicData.errorMessage();
        }
        String message = StrUtil.blankToDefault(musicData.errorMessage(),
                StrUtil.isBlank(audioUrl) ? "Suno 已完成但未返回音频地址" : "Suno 生成失败");
        return StrUtil.maxLength(message, 480);
    }

    private String downloadFile(Integer status, String url) {
        if (StrUtil.isBlank(url) || ObjectUtil.notEqual(status, AiMusicStatusEnum.SUCCESS.getStatus())) {
            return url;
        }
        try {
            byte[] bytes = remoteResourceService.download(url, MAX_MUSIC_DOWNLOAD_BYTES);
            return fileApi.createFile(bytes);
        } catch (Exception e) {
            log.error("[downloadFile][url({}) 下载失败]", url, e);
            return url;
        }
    }

}
