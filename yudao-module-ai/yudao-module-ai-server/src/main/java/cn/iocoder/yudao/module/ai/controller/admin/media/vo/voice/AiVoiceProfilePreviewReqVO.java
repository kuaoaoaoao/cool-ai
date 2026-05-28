package cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 音色试听 Request VO")
@Data
public class AiVoiceProfilePreviewReqVO {

    @Schema(description = "音色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "音色编号不能为空")
    private Long id;

    @Schema(description = "试听文本", requiredMode = Schema.RequiredMode.REQUIRED, example = "你好，欢迎使用 AI 音色库")
    @NotBlank(message = "试听文本不能为空")
    private String text;

    @Schema(description = "输出格式", example = "wav")
    private String format;

}
