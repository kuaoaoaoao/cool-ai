package cn.iocoder.yudao.module.ai.service.media.sensitive;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaToolAuditDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiMediaToolAuditMapper;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.*;

@Service
@Slf4j
public class AiSensitiveMediaControlServiceImpl implements AiSensitiveMediaControlService {

    @Resource
    private FileApi fileApi;
    @Resource
    private AiMediaToolAuditMapper mediaToolAuditMapper;
    @Autowired(required = false)
    private SecurityFrameworkService securityFrameworkService;

    @Value("${yudao.ai.media.sensitive.person-replacement-enable:false}")
    private Boolean personReplacementEnable;
    @Value("${yudao.ai.media.sensitive.local-voice-clone-enable:false}")
    private Boolean localVoiceCloneEnable;
    @Value("${yudao.ai.media.sensitive.local-voice-conversion-enable:false}")
    private Boolean localVoiceConversionEnable;
    @Value("${yudao.ai.media.sensitive.person-replacement-tenant-ids:}")
    private String personReplacementTenantIds;
    @Value("${yudao.ai.media.sensitive.local-voice-clone-tenant-ids:}")
    private String localVoiceCloneTenantIds;
    @Value("${yudao.ai.media.sensitive.local-voice-conversion-tenant-ids:}")
    private String localVoiceConversionTenantIds;
    @Value("${yudao.ai.media.sensitive.require-watermark:true}")
    private Boolean requireWatermark;
    @Value("${yudao.ai.media.sensitive.audit-enable:true}")
    private Boolean auditEnable;

    @Override
    public AiSensitiveMediaValidationResult validateCreate(AiSensitiveMediaCreateRequest request) {
        AiSensitiveMediaCapabilityEnum capability = AiSensitiveMediaCapabilityEnum.of(request.getCapability());
        try {
            validateEnabled(capability);
            validatePermission(capability);
            validateConfirmation(capability, request);
            validateSources(capability, request);
            AiSensitiveMediaValidationResult result = buildResult(capability, request);
            auditTaskEvent(capability.getCapability(), request.getUserId(), null, "create", true, null,
                    result.getAuditPayload());
            return result;
        } catch (RuntimeException e) {
            auditTaskEvent(capability.getCapability(), request.getUserId(), null, "create", false,
                    StrUtil.maxLength(e.getMessage(), 240), toAuditPayload(capability, request));
            throw e;
        }
    }

    @Override
    public void auditTaskEvent(String capability, Long userId, Long taskId, String event, boolean success,
                               String rejectReason, Object details) {
        if (!Boolean.TRUE.equals(auditEnable)) {
            return;
        }
        try {
            mediaToolAuditMapper.insert(new AiMediaToolAuditDO()
                    .setUserId(userId)
                    .setToolName("sensitive:" + capability + ":" + event)
                    .setTaskId(taskId)
                    .setSuccess(success)
                    .setRejectReason(rejectReason)
                    .setRequestJson(toMap(details)));
        } catch (Exception e) {
            log.warn("[auditTaskEvent][capability({}) taskId({}) event({}) 审计失败]", capability, taskId, event, e);
        }
    }

    private void validateEnabled(AiSensitiveMediaCapabilityEnum capability) {
        boolean enabled = switch (capability) {
            case PERSON_REPLACEMENT -> Boolean.TRUE.equals(personReplacementEnable);
            case LOCAL_VOICE_CLONE -> Boolean.TRUE.equals(localVoiceCloneEnable);
            case LOCAL_VOICE_CONVERSION -> Boolean.TRUE.equals(localVoiceConversionEnable);
        };
        if (!enabled || !isTenantEnabled(capability)) {
            throw exception(MEDIA_SENSITIVE_DISABLED, capability.getName());
        }
    }

    private boolean isTenantEnabled(AiSensitiveMediaCapabilityEnum capability) {
        Long tenantId = TenantContextHolder.getTenantId();
        String tenantIds = switch (capability) {
            case PERSON_REPLACEMENT -> personReplacementTenantIds;
            case LOCAL_VOICE_CLONE -> localVoiceCloneTenantIds;
            case LOCAL_VOICE_CONVERSION -> localVoiceConversionTenantIds;
        };
        if (StrUtil.isBlank(tenantIds)) {
            return false;
        }
        if (StrUtil.equals(tenantIds.trim(), "*")) {
            return true;
        }
        return tenantId != null && StrUtil.splitTrim(tenantIds, ',').contains(String.valueOf(tenantId));
    }

    private void validatePermission(AiSensitiveMediaCapabilityEnum capability) {
        if (securityFrameworkService == null || !securityFrameworkService.hasPermission(capability.getPermission())) {
            throw exception(MEDIA_SENSITIVE_FORBIDDEN, capability.getName());
        }
    }

    private void validateConfirmation(AiSensitiveMediaCapabilityEnum capability, AiSensitiveMediaCreateRequest request) {
        if (!Boolean.TRUE.equals(request.getConsentConfirmed()) || !Boolean.TRUE.equals(request.getRiskConfirmed())) {
            throw exception(MEDIA_SENSITIVE_CONFIRM_REQUIRED, capability.getName());
        }
    }

    private void validateSources(AiSensitiveMediaCapabilityEnum capability, AiSensitiveMediaCreateRequest request) {
        if (request.getSourceUrls() == null || request.getSourceUrls().isEmpty()) {
            throw exception(MEDIA_SENSITIVE_SOURCE_INVALID, "素材不能为空");
        }
        for (Map.Entry<String, String> entry : request.getSourceUrls().entrySet()) {
            String url = entry.getValue();
            if (!isProjectFileUrl(url)) {
                throw exception(MEDIA_SENSITIVE_SOURCE_INVALID, "敏感媒体素材必须使用项目文件");
            }
            FileRespDTO file = getSourceFile(url);
            if (StrUtil.isBlank(file.getCreator()) || !Objects.equals(file.getCreator(), String.valueOf(request.getUserId()))) {
                throw exception(MEDIA_SENSITIVE_SOURCE_FORBIDDEN);
            }
            String mimeType = StrUtil.blankToDefault(file.getType(),
                    request.getSourceMimeTypes() == null ? null : request.getSourceMimeTypes().get(entry.getKey()));
            if (StrUtil.isBlank(mimeType) || !capability.getSupportedMimeTypes().contains(mimeType.toLowerCase())) {
                throw exception(MEDIA_SENSITIVE_SOURCE_INVALID, "素材类型不支持：" + mimeType);
            }
        }
    }

    private AiSensitiveMediaValidationResult buildResult(AiSensitiveMediaCapabilityEnum capability,
                                                        AiSensitiveMediaCreateRequest request) {
        String traceId = StrUtil.blankToDefault(request.getTraceId(), "sensitive-" + UUID.randomUUID());
        boolean watermarkEnabled = !Boolean.FALSE.equals(request.getWatermarkEnabled())
                || Boolean.TRUE.equals(requireWatermark);
        Map<String, Object> auditPayload = toAuditPayload(capability, request);
        auditPayload.put("traceId", traceId);
        auditPayload.put("watermarkEnabled", watermarkEnabled);
        auditPayload.put("watermarkRequired", requireWatermark);
        auditPayload.put("validatedTime", LocalDateTime.now().toString());
        Map<String, Object> workerPayload = new LinkedHashMap<>();
        workerPayload.put("sensitiveCapability", capability.getCapability());
        workerPayload.put("sourceUrls", request.getSourceUrls());
        workerPayload.put("watermark", Map.of("enabled", watermarkEnabled, "traceId", traceId));
        return new AiSensitiveMediaValidationResult()
                .setCapability(capability.getCapability())
                .setPermission(capability.getPermission())
                .setWatermarkEnabled(watermarkEnabled)
                .setTraceId(traceId)
                .setAuditPayload(auditPayload)
                .setWorkerPayload(workerPayload);
    }

    private Map<String, Object> toAuditPayload(AiSensitiveMediaCapabilityEnum capability,
                                               AiSensitiveMediaCreateRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("capability", capability.getCapability());
        payload.put("provider", request.getProvider());
        payload.put("model", request.getModel());
        payload.put("tenantId", TenantContextHolder.getTenantId());
        payload.put("userId", request.getUserId());
        payload.put("sourceUrls", request.getSourceUrls());
        payload.put("consentConfirmed", request.getConsentConfirmed());
        payload.put("riskConfirmed", request.getRiskConfirmed());
        return payload;
    }

    private Map<String, Object> toMap(Object details) {
        if (details instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(Objects.toString(key), value));
            return result;
        }
        return Map.of("details", Objects.toString(details, ""));
    }

    private FileRespDTO getSourceFile(String sourceUrl) {
        try {
            return fileApi.getFileByUrl(sourceUrl).getCheckedData();
        } catch (Exception e) {
            throw exception(MEDIA_SENSITIVE_SOURCE_INVALID, "素材文件不存在");
        }
    }

    private boolean isProjectFileUrl(String sourceUrl) {
        try {
            String path = URI.create(sourceUrl.trim()).getPath();
            return StrUtil.contains(path, "/infra/file/") && StrUtil.contains(path, "/get/");
        } catch (Exception e) {
            return false;
        }
    }

}
