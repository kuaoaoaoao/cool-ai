package cn.iocoder.yudao.module.ai.dal.dataobject.media;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 媒体任务 DO。
 *
 * @author codex
 */
@TableName(value = "ai_media_task", autoResultMap = true)
@KeySequence("ai_media_task_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class AiMediaTaskDO extends TenantBaseDO {

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
     * 任务类型
     *
     * 枚举 {@link AiMediaTaskTypeEnum}
     */
    private Integer taskType;

    /**
     * 平台
     *
     * 枚举 {@link AiPlatformEnum}
     */
    private String platform;

    /**
     * 模型编号
     *
     * 关联 {@link AiModelDO#getId()}
     */
    private Long modelId;

    /**
     * 模型标识
     *
     * 冗余 {@link AiModelDO#getModel()}
     */
    private String model;

    /**
     * 任务状态
     *
     * 枚举 {@link AiMediaTaskStatusEnum}
     */
    private Integer status;

    /**
     * 任务进度：0-100
     */
    private Integer progress;

    /**
     * 当前阶段
     */
    private String stage;

    /**
     * 输入参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputJson;

    /**
     * 输出参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputJson;

    /**
     * 源文件编号列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> sourceFileIds;

    /**
     * 结果文件编号列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> resultFileIds;

    /**
     * Provider 侧任务编号
     */
    private String providerTaskId;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

}
