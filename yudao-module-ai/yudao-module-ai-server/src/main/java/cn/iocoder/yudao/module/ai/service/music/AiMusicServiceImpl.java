package cn.iocoder.yudao.module.ai.service.music;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ai.controller.admin.music.vo.AiMusicPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.music.vo.AiMusicUpdateMyReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.music.vo.AiMusicUpdateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.music.vo.AiSunoGenerateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.music.AiMusicDO;
import cn.iocoder.yudao.module.ai.dal.mysql.music.AiMusicMapper;
import cn.iocoder.yudao.module.ai.enums.music.AiMusicGenerateModeEnum;
import cn.iocoder.yudao.module.ai.enums.music.AiMusicStatusEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.ai.service.music.provider.AiMusicProviderAdapter;
import cn.iocoder.yudao.module.ai.service.music.provider.AiMusicProviderAdapterResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.MUSIC_GENERATE_PARAM_ERROR;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.MUSIC_NOT_EXISTS;

/**
 * AI 音乐 Service 实现类。
 *
 * 主服务只负责校验、选择模型、分发 Provider；具体平台差异放到 Provider 适配器中。
 *
 * @author xiaoxin
 */
@Service
@Slf4j
public class AiMusicServiceImpl implements AiMusicService {

    @Resource
    private AiModelService modelService;
    @Resource
    private AiMusicMapper musicMapper;
    @Resource
    private AiMusicProviderAdapterResolver providerAdapterResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generateMusic(Long userId, AiSunoGenerateReqVO reqVO) {
        validateGenerateReqVO(reqVO);
        AiModelDO model = fillSelectedModel(reqVO);
        AiMusicProviderAdapter adapter = providerAdapterResolver.resolveGenerate(model, reqVO);
        if (adapter == null) {
            throw exception(MUSIC_GENERATE_PARAM_ERROR,
                    StrUtil.format("音乐生成暂不支持平台({})模型({})", reqVO.getPlatform(), reqVO.getModel()));
        }
        return adapter.generate(userId, reqVO, model);
    }

    @Override
    public Integer syncMusic() {
        List<AiMusicDO> streamingTask = musicMapper.selectListByStatus(AiMusicStatusEnum.IN_PROGRESS.getStatus());
        if (CollUtil.isEmpty(streamingTask)) {
            return 0;
        }
        log.info("[syncMusic][开始同步, 共 ({}) 个任务]", streamingTask.size());

        Map<AiMusicProviderAdapter, List<AiMusicDO>> adapterTaskMap = new LinkedHashMap<>();
        streamingTask.forEach(music -> {
            AiMusicProviderAdapter adapter = providerAdapterResolver.resolveSync(music);
            if (adapter == null) {
                musicMapper.updateById(new AiMusicDO().setId(music.getId())
                        .setStatus(AiMusicStatusEnum.FAIL.getStatus())
                        .setErrorMessage(StrUtil.format("找不到音乐任务同步适配器，platform={}, taskId={}",
                                music.getPlatform(), music.getTaskId())));
                return;
            }
            adapterTaskMap.computeIfAbsent(adapter, key -> new java.util.ArrayList<>()).add(music);
        });
        adapterTaskMap.forEach((adapter, tasks) -> {
            try {
                adapter.sync(tasks);
            } catch (Exception e) {
                log.warn("[syncMusic][同步音乐任务失败, taskIds({})]", tasks.stream().map(AiMusicDO::getId).toList(), e);
                tasks.forEach(music -> musicMapper.updateById(new AiMusicDO()
                        .setId(music.getId())
                        .setStatus(AiMusicStatusEnum.FAIL.getStatus())
                        .setErrorMessage(StrUtil.maxLength(
                                StrUtil.format("同步音乐任务失败：{}", StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName())),
                                480))));
            }
        });
        return streamingTask.size();
    }

    @Override
    public void updateMusic(AiMusicUpdateReqVO updateReqVO) {
        validateMusicExists(updateReqVO.getId());
        musicMapper.updateById(new AiMusicDO().setId(updateReqVO.getId()).setPublicStatus(updateReqVO.getPublicStatus()));
    }

    @Override
    public void updateMyMusic(AiMusicUpdateMyReqVO updateReqVO, Long userId) {
        AiMusicDO musicDO = validateMusicExists(updateReqVO.getId());
        if (ObjUtil.notEqual(musicDO.getUserId(), userId)) {
            throw exception(MUSIC_NOT_EXISTS);
        }
        musicMapper.updateById(new AiMusicDO().setId(updateReqVO.getId()).setTitle(updateReqVO.getTitle()));
    }

    @Override
    public void deleteMusic(Long id) {
        validateMusicExists(id);
        musicMapper.deleteById(id);
    }

    @Override
    public void deleteMusicMy(Long id, Long userId) {
        AiMusicDO music = validateMusicExists(id);
        if (ObjUtil.notEqual(music.getUserId(), userId)) {
            throw exception(MUSIC_NOT_EXISTS);
        }
        musicMapper.deleteById(id);
    }

    @Override
    public AiMusicDO getMusic(Long id) {
        return musicMapper.selectById(id);
    }

    @Override
    public PageResult<AiMusicDO> getMusicPage(AiMusicPageReqVO pageReqVO) {
        return musicMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AiMusicDO> getMusicMyPage(AiMusicPageReqVO pageReqVO, Long userId) {
        return musicMapper.selectPageByMy(pageReqVO, userId);
    }

    private void validateGenerateReqVO(AiSunoGenerateReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getPlatform())) {
            throw exception(MUSIC_GENERATE_PARAM_ERROR, "平台不能为空");
        }
        if (Objects.equals(AiMusicGenerateModeEnum.DESCRIPTION.getMode(), reqVO.getGenerateMode())
                && StrUtil.isBlank(reqVO.getPrompt())) {
            throw exception(MUSIC_GENERATE_PARAM_ERROR, "描述模式下，音乐描述不能为空");
        }
        if (Objects.equals(AiMusicGenerateModeEnum.LYRIC.getMode(), reqVO.getGenerateMode())) {
            if (StrUtil.isBlank(reqVO.getPrompt())) {
                throw exception(MUSIC_GENERATE_PARAM_ERROR, "歌词模式下，歌词不能为空");
            }
            if (CollUtil.isEmpty(reqVO.getTags())) {
                throw exception(MUSIC_GENERATE_PARAM_ERROR, "歌词模式下，音乐风格不能为空");
            }
            if (StrUtil.isBlank(reqVO.getTitle())) {
                throw exception(MUSIC_GENERATE_PARAM_ERROR, "歌词模式下，歌曲名称不能为空");
            }
        }
    }

    private AiModelDO fillSelectedModel(AiSunoGenerateReqVO reqVO) {
        if (reqVO.getModelId() == null) {
            throw exception(MUSIC_GENERATE_PARAM_ERROR, "音乐生成必须选择模型管理中配置的模型");
        }
        AiModelDO model = modelService.validateModel(reqVO.getModelId(), AiModelTypeEnum.VOICE);
        reqVO.setPlatform(model.getPlatform());
        reqVO.setModel(model.getModel());
        return model;
    }

    private AiMusicDO validateMusicExists(Long id) {
        AiMusicDO music = musicMapper.selectById(id);
        if (music == null) {
            throw exception(MUSIC_NOT_EXISTS);
        }
        return music;
    }

}
