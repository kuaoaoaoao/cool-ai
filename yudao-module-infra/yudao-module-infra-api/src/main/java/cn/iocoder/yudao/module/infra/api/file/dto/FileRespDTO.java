package cn.iocoder.yudao.module.infra.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - 文件 Response DTO")
@Data
@Accessors(chain = true)
public class FileRespDTO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "配置编号", example = "1")
    private Long configId;

    @Schema(description = "原文件名", example = "source.png")
    private String name;

    @Schema(description = "文件路径", example = "ai-media/source/20260530/source.png")
    private String path;

    @Schema(description = "访问地址", example = "https://file.example.com/source.png")
    private String url;

    @Schema(description = "文件 MIME 类型", example = "image/png")
    private String type;

    @Schema(description = "文件大小", example = "2048000")
    private Long size;

    @Schema(description = "创建者", example = "1")
    private String creator;

}
