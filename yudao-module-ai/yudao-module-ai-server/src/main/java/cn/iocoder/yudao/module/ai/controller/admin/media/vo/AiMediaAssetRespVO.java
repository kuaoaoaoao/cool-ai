package cn.iocoder.yudao.module.ai.controller.admin.media.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 媒体资产 Response VO")
@Data
public class AiMediaAssetRespVO {

    @Schema(description = "资产类型", example = "RESULT_VIDEO")
    private String assetType;

    @Schema(description = "媒体任务编号", example = "1024")
    private Long taskId;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "任务类型", example = "3")
    private Integer taskType;

    @Schema(description = "任务状态", example = "50")
    private Integer status;

    @Schema(description = "平台", example = "SiliconFlow")
    private String platform;

    @Schema(description = "模型标识", example = "Wan-AI/Wan2.2-I2V-A14B")
    private String model;

    @Schema(description = "文件编号")
    private String fileId;

    @Schema(description = "文件链接")
    private String url;

    @Schema(description = "媒体类型", example = "video/mp4")
    private String mimeType;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "时长，单位：秒")
    private Integer duration;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否清理候选")
    private Boolean cleanupCandidate;

    @Schema(description = "清理状态", example = "ACTIVE")
    private String cleanupStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

}
