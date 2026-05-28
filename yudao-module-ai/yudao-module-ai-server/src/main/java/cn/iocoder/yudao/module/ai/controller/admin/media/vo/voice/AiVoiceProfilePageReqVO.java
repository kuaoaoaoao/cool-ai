package cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 音色库分页 Request VO")
@Data
public class AiVoiceProfilePageReqVO extends PageParam {

    @Schema(description = "音色名称", example = "我的声音")
    private String name;

    @Schema(description = "来源类型", example = "CLONE")
    @InEnum(AiVoiceProfileSourceTypeEnum.class)
    private String sourceType;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
