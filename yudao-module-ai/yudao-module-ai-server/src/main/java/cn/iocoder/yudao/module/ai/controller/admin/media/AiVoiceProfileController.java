package cn.iocoder.yudao.module.ai.controller.admin.media;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfilePageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfilePreviewReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfileRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfileSaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiVoiceProfileDO;
import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import cn.iocoder.yudao.module.ai.service.media.AiVoiceProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - AI 音色库")
@RestController
@RequestMapping("/ai/voice-profile")
public class AiVoiceProfileController {

    @Resource
    private AiVoiceProfileService voiceProfileService;
    @Resource
    private AiMediaTaskService mediaTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建【我的】音色")
    @PreAuthorize("@ss.hasPermission('ai:voice-profile:create')")
    public CommonResult<Long> createVoiceProfile(@Valid @RequestBody AiVoiceProfileSaveReqVO reqVO) {
        return success(voiceProfileService.createVoiceProfile(getLoginUserId(), reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新【我的】音色")
    @PreAuthorize("@ss.hasPermission('ai:voice-profile:update')")
    public CommonResult<Boolean> updateVoiceProfile(@Valid @RequestBody AiVoiceProfileSaveReqVO reqVO) {
        voiceProfileService.updateVoiceProfile(getLoginUserId(), reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除【我的】音色")
    @Parameter(name = "id", required = true, description = "音色编号", example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:voice-profile:delete')")
    public CommonResult<Boolean> deleteVoiceProfile(@RequestParam("id") Long id) {
        voiceProfileService.deleteVoiceProfile(getLoginUserId(), id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取【我的】音色")
    @Parameter(name = "id", required = true, description = "音色编号", example = "1024")
    public CommonResult<AiVoiceProfileRespVO> getVoiceProfile(@RequestParam("id") Long id) {
        AiVoiceProfileDO profile = voiceProfileService.getVoiceProfile(getLoginUserId(), id);
        return success(convert(profile));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获得【我的】音色分页")
    public CommonResult<PageResult<AiVoiceProfileRespVO>> getVoiceProfilePage(@Valid AiVoiceProfilePageReqVO pageReqVO) {
        PageResult<AiVoiceProfileDO> pageResult = voiceProfileService.getVoiceProfilePage(getLoginUserId(), pageReqVO);
        PageResult<AiVoiceProfileRespVO> result = BeanUtils.toBean(pageResult, AiVoiceProfileRespVO.class);
        result.getList().forEach(this::fillPreviewAudioUrl);
        return success(result);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得【我的】音色精简列表")
    public CommonResult<List<AiVoiceProfileRespVO>> getVoiceProfileSimpleList(@RequestParam(value = "status", required = false) Integer status) {
        List<AiVoiceProfileDO> list = voiceProfileService.getVoiceProfileSimpleList(getLoginUserId(), status);
        List<AiVoiceProfileRespVO> result = BeanUtils.toBean(list, AiVoiceProfileRespVO.class);
        result.forEach(this::fillPreviewAudioUrl);
        return success(result);
    }

    @PostMapping("/preview")
    @Operation(summary = "试听【我的】音色")
    @PreAuthorize("@ss.hasPermission('ai:voice-profile:preview')")
    public CommonResult<Long> previewVoiceProfile(@Valid @RequestBody AiVoiceProfilePreviewReqVO reqVO) {
        return success(voiceProfileService.previewVoiceProfile(getLoginUserId(), reqVO));
    }

    private AiVoiceProfileRespVO convert(AiVoiceProfileDO profile) {
        if (profile == null) {
            return null;
        }
        AiVoiceProfileRespVO respVO = BeanUtils.toBean(profile, AiVoiceProfileRespVO.class);
        fillPreviewAudioUrl(respVO);
        return respVO;
    }

    private void fillPreviewAudioUrl(AiVoiceProfileRespVO respVO) {
        if (respVO == null || respVO.getPreviewTaskId() == null) {
            return;
        }
        AiMediaTaskDO task = mediaTaskService.getTask(respVO.getPreviewTaskId());
        if (task != null && task.getOutputJson() != null) {
            Object url = task.getOutputJson().get("url");
            respVO.setPreviewAudioUrl(url != null ? url.toString() : null);
        }
    }

}
