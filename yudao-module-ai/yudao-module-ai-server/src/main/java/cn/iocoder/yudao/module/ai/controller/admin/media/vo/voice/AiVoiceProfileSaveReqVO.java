package cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 音色库保存 Request VO")
@Data
public class AiVoiceProfileSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "音色名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "我的声音")
    @NotBlank(message = "音色名称不能为空")
    private String name;

    @Schema(description = "来源类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CLONE")
    @NotBlank(message = "来源类型不能为空")
    @InEnum(AiVoiceProfileSourceTypeEnum.class)
    private String sourceType;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "模型编号不能为空")
    private Long modelId;

    @Schema(description = "预置音色", example = "冰糖")
    private String builtinVoice;

    @Schema(description = "文字设计音色描述", example = "年轻男性，声音清亮，科技播客主持人")
    private String designPrompt;

    @Schema(description = "克隆样本文件 URL", example = "https://www.iocoder.cn/sample.wav")
    private String sampleFileUrl;

    @Schema(description = "克隆样本 MIME 类型", example = "audio/wav")
    private String sampleMimeType;

    @Schema(description = "克隆样本大小", example = "2048000")
    private Long sampleSize;

    @Schema(description = "是否确认已获得声音授权", example = "true")
    private Boolean consentConfirmed;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "客服声音样本")
    private String remark;

}
