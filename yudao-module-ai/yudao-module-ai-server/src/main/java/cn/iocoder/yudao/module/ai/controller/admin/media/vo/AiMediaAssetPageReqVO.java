package cn.iocoder.yudao.module.ai.controller.admin.media.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - AI 媒体资产分页 Request VO")
@Data
public class AiMediaAssetPageReqVO extends PageParam {

    @Schema(description = "资产类型", example = "RESULT_VIDEO")
    private String assetType;

    @Schema(description = "媒体任务编号", example = "1024")
    private Long taskId;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "任务类型", example = "3")
    @InEnum(AiMediaTaskTypeEnum.class)
    private Integer taskType;

    @Schema(description = "任务状态", example = "50")
    @InEnum(AiMediaTaskStatusEnum.class)
    private Integer status;

    @Schema(description = "平台", example = "SiliconFlow")
    private String platform;

    @Schema(description = "仅查询可清理候选资产", example = "true")
    private Boolean cleanupCandidate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
