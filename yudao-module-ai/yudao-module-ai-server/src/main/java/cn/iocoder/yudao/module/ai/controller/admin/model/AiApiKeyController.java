package cn.iocoder.yudao.module.ai.controller.admin.model;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.AiModelConnectionTestRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.apikey.AiApiKeyPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.apikey.AiApiKeyRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.apikey.AiApiKeySaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
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
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.API_KEY_NOT_EXISTS;

@Tag(name = "管理后台 - AI API 密钥")
@RestController
@RequestMapping("/ai/api-key")
@Validated
public class AiApiKeyController {

    @Resource
    private AiApiKeyService apiKeyService;
    @Resource
    private AiModelService modelService;

    @PostMapping("/create")
    @Operation(summary = "创建 API 密钥")
    @PreAuthorize("@ss.hasPermission('ai:api-key:create')")
    public CommonResult<Long> createApiKey(@Valid @RequestBody AiApiKeySaveReqVO createReqVO) {
        return success(apiKeyService.createApiKey(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 API 密钥")
    @PreAuthorize("@ss.hasPermission('ai:api-key:update')")
    public CommonResult<Boolean> updateApiKey(@Valid @RequestBody AiApiKeySaveReqVO updateReqVO) {
        apiKeyService.updateApiKey(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 API 密钥")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:api-key:delete')")
    public CommonResult<Boolean> deleteApiKey(@RequestParam("id") Long id) {
        apiKeyService.deleteApiKey(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 API 密钥")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:api-key:query')")
    public CommonResult<AiApiKeyRespVO> getApiKey(@RequestParam("id") Long id) {
        AiApiKeyDO apiKey = apiKeyService.getApiKey(id);
        if (apiKey == null) {
            throw exception(API_KEY_NOT_EXISTS);
        }
        AiApiKeyRespVO respVO = BeanUtils.toBean(apiKey, AiApiKeyRespVO.class);
        maskApiKey(respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 API 密钥分页")
    @PreAuthorize("@ss.hasPermission('ai:api-key:query')")
    public CommonResult<PageResult<AiApiKeyRespVO>> getApiKeyPage(@Valid AiApiKeyPageReqVO pageReqVO) {
        PageResult<AiApiKeyDO> pageResult = apiKeyService.getApiKeyPage(pageReqVO);
        PageResult<AiApiKeyRespVO> result = BeanUtils.toBean(pageResult, AiApiKeyRespVO.class);
        result.getList().forEach(this::maskApiKey);
        return success(result);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得 API 密钥分页列表")
    @PreAuthorize("@ss.hasPermission('ai:api-key:query') || @ss.hasPermission('ai:model:query')")
    public CommonResult<List<AiApiKeyRespVO>> getApiKeySimpleList() {
        List<AiApiKeyDO> list = apiKeyService.getApiKeyList();
        return success(convertList(list, key -> {
            AiApiKeyRespVO respVO = BeanUtils.toBean(key, AiApiKeyRespVO.class);
            respVO.setApiKey(null);
            return respVO;
        }));
    }

    @PostMapping("/test-connection")
    @Operation(summary = "测试 API 密钥连接")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:api-key:update') || @ss.hasPermission('ai:api-key:query')")
    public CommonResult<AiModelConnectionTestRespVO> testApiKeyConnection(@RequestParam("id") Long id) {
        return success(modelService.testApiKeyConnection(id));
    }

    private void maskApiKey(AiApiKeyRespVO respVO) {
        respVO.setApiKey(respVO.getApiKey() == null ? null : "******");
    }

}
