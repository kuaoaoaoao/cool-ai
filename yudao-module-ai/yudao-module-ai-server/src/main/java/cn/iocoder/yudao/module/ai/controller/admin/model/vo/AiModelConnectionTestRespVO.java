package cn.iocoder.yudao.module.ai.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - AI 模型连接测试 Response VO")
@Data
@Accessors(chain = true)
public class AiModelConnectionTestRespVO {

    @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean success;

    @Schema(description = "测试对象", example = "MODEL")
    private String target;

    @Schema(description = "模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "API Key 编号", example = "2048")
    private Long keyId;

    @Schema(description = "平台", example = "OpenAI")
    private String platform;

    @Schema(description = "模型类型", example = "1")
    private Integer type;

    @Schema(description = "模型标识", example = "gpt-4o-mini")
    private String model;

    @Schema(description = "耗时，单位毫秒", example = "580")
    private Long durationMillis;

    @Schema(description = "结果提示", example = "连接测试成功")
    private String message;

    @Schema(description = "错误码", example = "AUTH_ERROR")
    private String errorCode;

    @Schema(description = "错误详情")
    private String detail;

}
