package cn.iocoder.yudao.module.ai.service.media.sensitive;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaToolAuditDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiMediaToolAuditMapper;
import cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiSensitiveMediaControlServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiSensitiveMediaControlServiceImpl service;

    @Mock
    private FileApi fileApi;
    @Mock
    private AiMediaToolAuditMapper mediaToolAuditMapper;
    @Mock
    private SecurityFrameworkService securityFrameworkService;

    private static final String SOURCE_URL = "http://localhost:48080/admin-api/infra/file/1/get/source.mp4";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(service, "personReplacementEnable", true);
        ReflectionTestUtils.setField(service, "personReplacementTenantIds", "1");
        ReflectionTestUtils.setField(service, "localVoiceCloneEnable", false);
        ReflectionTestUtils.setField(service, "localVoiceCloneTenantIds", "");
        ReflectionTestUtils.setField(service, "localVoiceConversionEnable", false);
        ReflectionTestUtils.setField(service, "localVoiceConversionTenantIds", "");
        ReflectionTestUtils.setField(service, "requireWatermark", true);
        ReflectionTestUtils.setField(service, "auditEnable", true);
        lenient().when(securityFrameworkService.hasPermission("ai:media:person-replacement:create")).thenReturn(true);
        lenient().when(fileApi.getFileByUrl(SOURCE_URL)).thenReturn(CommonResult.success(new FileRespDTO()
                .setUrl(SOURCE_URL)
                .setType("video/mp4")
                .setCreator("10")));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void validateCreate_success_auditsAndBuildsWorkerPayload() {
        AiSensitiveMediaValidationResult result = service.validateCreate(buildRequest());

        assertEquals(AiSensitiveMediaCapabilityEnum.PERSON_REPLACEMENT.getCapability(), result.getCapability());
        assertTrue(result.getWatermarkEnabled());
        assertNotNull(result.getTraceId());
        assertEquals("PERSON_REPLACEMENT", result.getWorkerPayload().get("sensitiveCapability"));
        verify(mediaToolAuditMapper).insert(argThat((AiMediaToolAuditDO audit) ->
                Boolean.TRUE.equals(audit.getSuccess())
                        && "sensitive:PERSON_REPLACEMENT:create".equals(audit.getToolName())
                        && audit.getRequestJson().containsKey("watermarkEnabled")));
    }

    @Test
    void validateCreate_disabled() {
        ReflectionTestUtils.setField(service, "personReplacementEnable", false);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.validateCreate(buildRequest()));

        assertEquals(ErrorCodeConstants.MEDIA_SENSITIVE_DISABLED.getCode(), exception.getCode());
        verify(mediaToolAuditMapper).insert(argThat((AiMediaToolAuditDO audit) ->
                Boolean.FALSE.equals(audit.getSuccess())));
    }

    @Test
    void validateCreate_missingPermission() {
        when(securityFrameworkService.hasPermission("ai:media:person-replacement:create")).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.validateCreate(buildRequest()));

        assertEquals(ErrorCodeConstants.MEDIA_SENSITIVE_FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void validateCreate_missingConfirmation() {
        AiSensitiveMediaCreateRequest request = buildRequest().setRiskConfirmed(false);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.validateCreate(request));

        assertEquals(ErrorCodeConstants.MEDIA_SENSITIVE_CONFIRM_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void validateCreate_unauthorizedSource() {
        when(fileApi.getFileByUrl(SOURCE_URL)).thenReturn(CommonResult.success(new FileRespDTO()
                .setUrl(SOURCE_URL)
                .setType("video/mp4")
                .setCreator("11")));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.validateCreate(buildRequest()));

        assertEquals(ErrorCodeConstants.MEDIA_SENSITIVE_SOURCE_FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void validateCreate_unsupportedMimeType() {
        when(fileApi.getFileByUrl(SOURCE_URL)).thenReturn(CommonResult.success(new FileRespDTO()
                .setUrl(SOURCE_URL)
                .setType("application/pdf")
                .setCreator("10")));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.validateCreate(buildRequest()));

        assertEquals(ErrorCodeConstants.MEDIA_SENSITIVE_SOURCE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void auditTaskEvent_success() {
        service.auditTaskEvent("PERSON_REPLACEMENT", 10L, 300L, "succeed", true, null,
                Map.of("traceId", "trace-1"));

        ArgumentCaptor<AiMediaToolAuditDO> captor = ArgumentCaptor.forClass(AiMediaToolAuditDO.class);
        verify(mediaToolAuditMapper).insert(captor.capture());
        assertEquals("sensitive:PERSON_REPLACEMENT:succeed", captor.getValue().getToolName());
        assertEquals(300L, captor.getValue().getTaskId());
        assertEquals("trace-1", captor.getValue().getRequestJson().get("traceId"));
    }

    private AiSensitiveMediaCreateRequest buildRequest() {
        return new AiSensitiveMediaCreateRequest()
                .setCapability(AiSensitiveMediaCapabilityEnum.PERSON_REPLACEMENT.getCapability())
                .setUserId(10L)
                .setProvider("PythonWorker")
                .setModel("mock-person-replacement")
                .setConsentConfirmed(true)
                .setRiskConfirmed(true)
                .setWatermarkEnabled(false)
                .setSourceUrls(Map.of("sourceVideo", SOURCE_URL));
    }

}
