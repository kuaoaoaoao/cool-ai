package cn.iocoder.yudao.module.ai.controller.admin.media.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - AI 媒体任务 Response VO")
@Data
public class AiMediaTaskRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long userId;

    @Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer taskType;

    @Schema(description = "平台", requiredMode = Schema.RequiredMode.REQUIRED, example = "XiaomiMiMo")
    private String platform;

    @Schema(description = "模型编号", example = "2048")
    private Long modelId;

    @Schema(description = "模型标识", example = "mimo-v2.5-tts")
    private String model;

    @Schema(description = "任务状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private Integer status;

    @Schema(description = "任务进度", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer progress;

    @Schema(description = "当前阶段", example = "done")
    private String stage;

    @Schema(description = "输入参数")
    private Map<String, Object> inputJson;

    @Schema(description = "输出参数")
    private Map<String, Object> outputJson;

    @Schema(description = "源文件编号列表")
    private List<String> sourceFileIds;

    @Schema(description = "结果文件编号列表")
    private List<String> resultFileIds;

    @Schema(description = "Provider 任务编号")
    private String providerTaskId;

    @Schema(description = "执行模式", example = "PROVIDER_ASYNC")
    private String executionMode;

    @Schema(description = "重试次数", example = "1")
    private Integer retryCount;

    @Schema(description = "是否已请求取消", example = "true")
    private Boolean cancelRequested;

    @Schema(description = "提交时间")
    private String submittedTime;

    @Schema(description = "回调状态", example = "NONE")
    private String callbackStatus;

    @Schema(description = "源资产")
    private Object sourceAssets;

    @Schema(description = "结果资产")
    private Object resultAssets;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
