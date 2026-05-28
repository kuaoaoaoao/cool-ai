package cn.iocoder.yudao.module.ai.controller.admin.media.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - AI 媒体 TTS 创建 Request VO")
@Data
@Accessors(chain = true)
public class AiMediaTtsCreateReqVO {

    @Schema(description = "模型编号；为空时使用默认语音模型", example = "1024")
    private Long modelId;

    @Schema(description = "音色库编号；为空时使用临时参数", example = "2048")
    private Long voiceProfileId;

    @Schema(description = "音色来源类型", example = "CLONE")
    @InEnum(AiVoiceProfileSourceTypeEnum.class)
    private String voiceMode;

    @Schema(description = "合成文本", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "欢迎使用 AI 媒体平台")
    @NotBlank(message = "合成文本不能为空")
    private String text;

    @Schema(description = "说话风格/情绪提示", example = "温柔、自然、有亲和力")
    private String stylePrompt;

    @Schema(description = "音色", requiredMode = Schema.RequiredMode.REQUIRED, example = "mimo_default")
    private String voice;

    @Schema(description = "文字设计音色描述", example = "年轻男性，声音清亮，像科技播客主持人")
    private String designPrompt;

    @Schema(description = "克隆样本文件 URL", example = "https://www.iocoder.cn/sample.wav")
    private String sampleFileUrl;

    @Schema(description = "克隆样本 MIME 类型", example = "audio/wav")
    private String sampleMimeType;

    @Schema(description = "克隆样本大小", example = "2048000")
    private Long sampleSize;

    @Schema(description = "是否确认已获得声音授权", example = "true")
    private Boolean consentConfirmed;

    @Schema(description = "音频格式", requiredMode = Schema.RequiredMode.REQUIRED, example = "wav")
    @NotBlank(message = "音频格式不能为空")
    private String format;

}
