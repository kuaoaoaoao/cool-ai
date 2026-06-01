package cn.iocoder.yudao.module.ai.controller.admin.media;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaImageToVideoCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @PostMapping("/image-to-video")
    @Operation(summary = "创建图生视频媒体任务")
    @PreAuthorize("@ss.hasPermission('ai:media:video:create')")
    public CommonResult<Long> createImageToVideoTask(@Valid @RequestBody AiMediaImageToVideoCreateReqVO reqVO) {
        return success(mediaTaskService.createImageToVideoTask(getLoginUserId(), reqVO));
    }

    @PostMapping("/callback/python-worker")
    @Operation(summary = "接收 Python Worker 媒体任务回调")
    @PermitAll
    public CommonResult<Boolean> handlePythonWorkerCallback(
            @RequestHeader(value = "X-Worker-Token", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        mediaTaskService.handlePythonWorkerCallback(token, payload);
        return success(true);
    }

    @GetMapping("/get-my")
    @Operation(summary = "获取【我的】媒体任务状态")
    @Parameter(name = "id", required = true, description = "媒体任务编号", example = "1024")
    public CommonResult<AiMediaTaskRespVO> getMyTask(@RequestParam("id") Long id) {
        AiMediaTaskDO task = mediaTaskService.getUserTaskAndRefresh(id, getLoginUserId());
        return success(buildRespVO(task));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获得【我的】媒体任务分页")
    public CommonResult<PageResult<AiMediaTaskRespVO>> getMyTaskPage(@Valid AiMediaTaskPageReqVO pageReqVO) {
        PageResult<AiMediaTaskDO> pageResult = mediaTaskService.getUserTaskPage(pageReqVO, getLoginUserId());
        return success(buildPageRespVO(pageResult));
    }

    @GetMapping("/page")
    @Operation(summary = "获得媒体任务分页")
    @PreAuthorize("@ss.hasPermission('ai:media:task:query')")
    public CommonResult<PageResult<AiMediaTaskRespVO>> getTaskPage(@Valid AiMediaTaskPageReqVO pageReqVO) {
        PageResult<AiMediaTaskDO> pageResult = mediaTaskService.getTaskPage(pageReqVO);
        return success(buildPageRespVO(pageResult));
    }

    @GetMapping("/my-assets")
    @Operation(summary = "获得【我的】媒体资产聚合分页")
    public CommonResult<PageResult<AiMediaAssetRespVO>> getMyAssetPage(@Valid AiMediaAssetPageReqVO pageReqVO) {
        return success(mediaTaskService.getUserAssetPage(pageReqVO, getLoginUserId()));
    }

    @GetMapping("/assets")
    @Operation(summary = "获得媒体资产聚合分页")
    @PreAuthorize("@ss.hasPermission('ai:media:asset:query')")
    public CommonResult<PageResult<AiMediaAssetRespVO>> getAssetPage(@Valid AiMediaAssetPageReqVO pageReqVO) {
        return success(mediaTaskService.getAssetPage(pageReqVO));
    }

    @GetMapping("/my-assets-by-task")
    @Operation(summary = "获得【我的】任务关联资产")
    @Parameter(name = "taskId", required = true, description = "媒体任务编号", example = "1024")
    public CommonResult<List<AiMediaAssetRespVO>> getMyTaskAssets(@RequestParam("taskId") Long taskId) {
        return success(mediaTaskService.getUserTaskAssets(taskId, getLoginUserId()));
    }

    @GetMapping("/assets-by-task")
    @Operation(summary = "获得任务关联资产")
    @Parameter(name = "taskId", required = true, description = "媒体任务编号", example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:media:asset:query')")
    public CommonResult<List<AiMediaAssetRespVO>> getTaskAssets(@RequestParam("taskId") Long taskId) {
        return success(mediaTaskService.getTaskAssets(taskId));
    }

    @PostMapping("/retry-my")
    @Operation(summary = "重试【我的】媒体任务")
    @Parameter(name = "id", required = true, description = "媒体任务编号", example = "1024")
    public CommonResult<Boolean> retryMyTask(@RequestParam("id") Long id) {
        mediaTaskService.retryUserTask(id, getLoginUserId());
        return success(true);
    }

    @PostMapping("/cancel-my")
    @Operation(summary = "取消【我的】媒体任务")
    @Parameter(name = "id", required = true, description = "媒体任务编号", example = "1024")
    public CommonResult<Boolean> cancelMyTask(@RequestParam("id") Long id) {
        mediaTaskService.cancelUserTask(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/provider-health")
    @Operation(summary = "检查媒体 Provider 健康状态")
    @PreAuthorize("@ss.hasPermission('ai:media:video:create')")
    public CommonResult<Map<String, Object>> getProviderHealth() {
        return success(mediaTaskService.getProviderHealth());
    }

    private PageResult<AiMediaTaskRespVO> buildPageRespVO(PageResult<AiMediaTaskDO> pageResult) {
        List<AiMediaTaskRespVO> list = pageResult.getList() == null ? List.of()
                : pageResult.getList().stream().map(this::buildRespVO).toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    private AiMediaTaskRespVO buildRespVO(AiMediaTaskDO task) {
        AiMediaTaskRespVO respVO = BeanUtils.toBean(task, AiMediaTaskRespVO.class);
        Map<String, Object> inputJson = task.getInputJson();
        Map<String, Object> outputJson = task.getOutputJson();
        if (inputJson != null) {
            respVO.setSourceAssets(inputJson.get("sourceAsset"));
        }
        if (outputJson != null) {
            respVO.setExecutionMode(toString(outputJson.get("executionMode")));
            respVO.setRetryCount(toInteger(outputJson.get("retryCount")));
            respVO.setCancelRequested(Boolean.TRUE.equals(outputJson.get("cancelRequested")));
            respVO.setSubmittedTime(toString(outputJson.get("submittedTime")));
            respVO.setCallbackStatus(toString(outputJson.get("callbackStatus")));
            respVO.setResultAssets(outputJson.get("resultAsset"));
        }
        return respVO;
    }

    private String toString(Object value) {
        return value == null ? null : Objects.toString(value);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(Objects.toString(value));
        } catch (Exception e) {
            return null;
        }
    }

}
