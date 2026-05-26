package cn.iocoder.yudao.module.ai.controller.admin.media;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - AI 媒体任务")
@RestController
@RequestMapping("/ai/media/task")
public class AiMediaTaskController {

    @Resource
    private AiMediaTaskService mediaTaskService;

    @PostMapping("/tts")
    @Operation(summary = "创建 TTS 媒体任务")
    @PreAuthorize("@ss.hasPermission('ai:media:tts:create')")
    public CommonResult<Long> createTtsTask(@Valid @RequestBody AiMediaTtsCreateReqVO reqVO) {
        return success(mediaTaskService.createTtsTask(getLoginUserId(), reqVO));
    }

    @GetMapping("/get-my")
    @Operation(summary = "获取【我的】媒体任务状态")
    @Parameter(name = "id", required = true, description = "媒体任务编号", example = "1024")
    public CommonResult<AiMediaTaskRespVO> getMyTask(@RequestParam("id") Long id) {
        AiMediaTaskDO task = mediaTaskService.getUserTask(id, getLoginUserId());
        return success(BeanUtils.toBean(task, AiMediaTaskRespVO.class));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获得【我的】媒体任务分页")
    public CommonResult<PageResult<AiMediaTaskRespVO>> getMyTaskPage(@Valid AiMediaTaskPageReqVO pageReqVO) {
        PageResult<AiMediaTaskDO> pageResult = mediaTaskService.getUserTaskPage(pageReqVO, getLoginUserId());
        return success(BeanUtils.toBean(pageResult, AiMediaTaskRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得媒体任务分页")
    @PreAuthorize("@ss.hasPermission('ai:media:task:query')")
    public CommonResult<PageResult<AiMediaTaskRespVO>> getTaskPage(@Valid AiMediaTaskPageReqVO pageReqVO) {
        PageResult<AiMediaTaskDO> pageResult = mediaTaskService.getTaskPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AiMediaTaskRespVO.class));
    }

}
