package cn.iocoder.yudao.module.ai.dal.dataobject.media;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "ai_media_tool_audit", autoResultMap = true)
@KeySequence("ai_media_tool_audit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AiMediaToolAuditDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String toolName;

    private Long taskId;

    private Boolean success;

    private String rejectReason;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> requestJson;

}
