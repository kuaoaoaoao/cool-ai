package cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 音色库 Response VO")
@Data
public class AiVoiceProfileRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "音色名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "我的声音")
    private String name;

    @Schema(description = "来源类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CLONE")
    private String sourceType;

    @Schema(description = "平台", requiredMode = Schema.RequiredMode.REQUIRED, example = "XiaomiMiMo")
    private String platform;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long modelId;

    @Schema(description = "模型标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "mimo-v2.5-tts-voiceclone")
    private String model;

    @Schema(description = "预置音色", example = "冰糖")
    private String builtinVoice;

    @Schema(description = "文字设计音色描述")
    private String designPrompt;

    @Schema(description = "克隆样本文件 URL")
    private String sampleFileUrl;

    @Schema(description = "克隆样本 MIME 类型")
    private String sampleMimeType;

    @Schema(description = "克隆样本大小")
    private Long sampleSize;

    @Schema(description = "是否确认已获得声音授权")
    private Boolean consentConfirmed;

    @Schema(description = "试听任务编号")
    private Long previewTaskId;

    @Schema(description = "试听音频 URL")
    private String previewAudioUrl;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
