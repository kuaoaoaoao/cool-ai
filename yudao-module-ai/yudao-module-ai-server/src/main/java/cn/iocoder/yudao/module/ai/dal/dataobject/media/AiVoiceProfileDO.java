package cn.iocoder.yudao.module.ai.dal.dataobject.media;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 音色库 DO。
 *
 * @author codex
 */
@TableName("ai_voice_profile")
@KeySequence("ai_voice_profile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AiVoiceProfileDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 音色名称
     */
    private String name;

    /**
     * 来源类型
     *
     * 枚举 {@link AiVoiceProfileSourceTypeEnum}
     */
    private String sourceType;

    /**
     * 平台
     *
     * 枚举 {@link AiPlatformEnum}
     */
    private String platform;

    /**
     * 模型编号
     */
    private Long modelId;

    /**
     * 模型标识
     */
    private String model;

    /**
     * 预置音色
     */
    private String builtinVoice;

    /**
     * 文字设计音色描述
     */
    private String designPrompt;

    /**
     * 克隆样本文件 URL
     */
    private String sampleFileUrl;

    /**
     * 克隆样本 MIME 类型
     */
    private String sampleMimeType;

    /**
     * 克隆样本大小
     */
    private Long sampleSize;

    /**
     * 是否已确认授权
     */
    private Boolean consentConfirmed;

    /**
     * 试听任务编号
     */
    private Long previewTaskId;

    /**
     * 状态
     *
     * 枚举 CommonStatusEnum
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
