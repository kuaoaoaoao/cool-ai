package cn.iocoder.yudao.module.ai.controller.admin.media.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - AI 媒体 TTS 创建 Request VO")
@Data
public class AiMediaTtsCreateReqVO {

    @Schema(description = "模型编号；为空时使用默认语音模型", example = "1024")
    private Long modelId;

    @Schema(description = "合成文本", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "欢迎使用 AI 媒体平台")
    @NotBlank(message = "合成文本不能为空")
    private String text;

    @Schema(description = "说话风格/情绪提示", example = "温柔、自然、有亲和力")
    private String stylePrompt;

    @Schema(description = "音色", requiredMode = Schema.RequiredMode.REQUIRED, example = "mimo_default")
    @NotBlank(message = "音色不能为空")
    private String voice;

    @Schema(description = "音频格式", requiredMode = Schema.RequiredMode.REQUIRED, example = "wav")
    @NotBlank(message = "音频格式不能为空")
    private String format;

}
