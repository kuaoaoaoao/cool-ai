package cn.iocoder.yudao.module.ai.controller.admin.media.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.URL;

@Schema(description = "管理后台 - AI 媒体图生视频创建 Request VO")
@Data
@Accessors(chain = true)
public class AiMediaImageToVideoCreateReqVO {

    @Schema(description = "模型编号；为空时使用默认视频模型", example = "1024")
    private Long modelId;

    @Schema(description = "源图片 URL", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://www.iocoder.cn/source.png")
    @NotBlank(message = "源图片 URL 不能为空")
    @URL(message = "源图片 URL 格式不正确")
    private String sourceImageUrl;

    @Schema(description = "源图片 MIME 类型", example = "image/png")
    private String sourceMimeType;

    @Schema(description = "源图片大小", example = "2048000")
    private Long sourceSize;

    @Schema(description = "提示词", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "镜头缓慢推进，人物微笑，电影质感")
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    @Schema(description = "负向提示词", example = "模糊、变形、低清晰度")
    private String negativePrompt;

    @Schema(description = "视频尺寸", example = "1280x720")
    private String imageSize;

    @Schema(description = "随机种子", example = "123456")
    private Long seed;

    @Schema(description = "时长，秒；SiliconFlow MVP 先记录参数，Provider 是否支持由模型决定", example = "5")
    private Integer duration;

    @Schema(description = "质量；SiliconFlow MVP 先记录参数，Provider 是否支持由模型决定", example = "standard")
    private String quality;

}
