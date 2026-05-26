package cn.iocoder.yudao.module.ai.controller.admin.media.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - AI 媒体任务创建 Request VO")
@Data
public class AiMediaTaskCreateReqVO {

    @Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "任务类型不能为空")
    @InEnum(AiMediaTaskTypeEnum.class)
    private Integer taskType;

    @Schema(description = "输入参数")
    private Map<String, Object> inputJson;

}
