package cn.iocoder.yudao.module.ai.controller.admin.model;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.model.AiModelPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.AiModelConnectionTestRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.model.AiModelRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.model.AiModelSaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiModelCapabilities;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiModelCapability;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - AI 模型")
@RestController
@RequestMapping("/ai/model")
@Validated
public class AiModelController {

    @Resource
    private AiModelService modelService;

    @PostMapping("/create")
    @Operation(summary = "创建模型")
    @PreAuthorize("@ss.hasPermission('ai:model:create')")
    public CommonResult<Long> createModel(@Valid @RequestBody AiModelSaveReqVO createReqVO) {
        return success(modelService.createModel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新模型")
    @PreAuthorize("@ss.hasPermission('ai:model:update')")
    public CommonResult<Boolean> updateModel(@Valid @RequestBody AiModelSaveReqVO updateReqVO) {
        modelService.updateModel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除模型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:model:delete')")
    public CommonResult<Boolean> deleteModel(@RequestParam("id") Long id) {
        modelService.deleteModel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得模型")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:model:query')")
    public CommonResult<AiModelRespVO> getModel(@RequestParam("id") Long id) {
        AiModelDO model = modelService.getModel(id);
        return success(buildModelRespVO(model));
    }

    @GetMapping("/page")
    @Operation(summary = "获得模型分页")
    @PreAuthorize("@ss.hasPermission('ai:model:query')")
    public CommonResult<PageResult<AiModelRespVO>> getModelPage(@Valid AiModelPageReqVO pageReqVO) {
        PageResult<AiModelDO> pageResult = modelService.getModelPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AiModelRespVO.class, this::fillModelCapabilities));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得模型列表")
    @Parameter(name = "type", description = "类型", required = true, example = "1")
    @Parameter(name = "platform", description = "平台", example = "midjourney")
    public CommonResult<List<AiModelRespVO>> getModelSimpleList(
            @RequestParam("type") Integer type,
            @RequestParam(value = "platform", required = false) String platform) {
        List<AiModelDO> list = modelService.getModelListByStatusAndType(
                CommonStatusEnum.ENABLE.getStatus(), type, platform);
        return success(convertList(list, this::buildModelRespVO));
    }

    @PostMapping("/test-connection")
    @Operation(summary = "测试模型连接")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:model:update') || @ss.hasPermission('ai:model:query')")
    public CommonResult<AiModelConnectionTestRespVO> testModelConnection(@RequestParam("id") Long id) {
        return success(modelService.testModelConnection(id));
    }

    private AiModelRespVO buildModelRespVO(AiModelDO model) {
        return fillModelCapabilities(BeanUtils.toBean(model, AiModelRespVO.class));
    }

    private AiModelRespVO fillModelCapabilities(AiModelRespVO respVO) {
        AiModelDO model = BeanUtils.toBean(respVO, AiModelDO.class);
        AiModelCapability capability = AiModelCapabilities.infer(model);
        return respVO.setCapabilities(capability.getCapabilities())
                .setInputModalities(capability.getInputModalities())
                .setOutputModalities(capability.getOutputModalities())
                .setConfigTips(capability.getConfigTips());
    }

}
