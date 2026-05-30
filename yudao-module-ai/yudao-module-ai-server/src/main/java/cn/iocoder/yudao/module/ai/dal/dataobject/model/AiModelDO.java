package cn.iocoder.yudao.module.ai.dal.dataobject.model;

import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * AI 模型 DO
 *
 * 默认模型：{@link #status} 为开启，并且 {@link #sort} 排序第一
 *
 * @author fansili
 * @since 2024/4/24 19:39
 */
@TableName(value = "ai_model", autoResultMap = true)
@KeySequence("ai_model_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * API 秘钥编号
     *
     * 关联 {@link AiApiKeyDO#getId()}
     */
    private Long keyId;
    /**
     * 模型名称
     */
    private String name;
    /**
     * 模型标志
     */
    private String model;
    /**
     * 平台
     *
     * 枚举 {@link AiPlatformEnum}
     */
    private String platform;
    /**
     * 类型
     *
     * 枚举 {@link AiModelTypeEnum}
     */
    private Integer type;

    /**
     * 排序值
     */
    private Integer sort;
    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    // ========== 能力配置 ==========

    /**
     * 能力标签，例如 chat、text-generation、image-generation、image-to-video、tts、embedding、rerank。
     *
     * 为空时，兼容旧数据，通过平台、类型、模型标识进行推断。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> capabilities;

    /**
     * 输入模态，例如 text、image、audio、video。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> inputModalities;

    /**
     * 输出模态，例如 text、image、audio、video、vector、score。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> outputModalities;

    /**
     * 模型默认参数，不同能力或 Provider 可保存自己的结构。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> options;

    /**
     * 模型限制信息，例如最大 token、最大文件大小、最大视频时长。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> limits;

    /**
     * 扩展元数据，例如协议版本、计费单位、厂商特殊能力。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    // ========== 对话配置 ==========

    /**
     * 温度参数
     *
     * 用于调整生成回复的随机性和多样性程度：较低的温度值会使输出更收敛于高频词汇，较高的则增加多样性
     */
    private Double temperature;
    /**
     * 单条回复的最大 Token 数量
     */
    private Integer maxTokens;
    /**
     * 上下文的最大 Message 数量
     */
    private Integer maxContexts;

}
