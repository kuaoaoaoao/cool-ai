package cn.iocoder.yudao.module.ai.service.media;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaImageToVideoCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiVoiceProfileDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiMediaTaskMapper;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiVoiceProfileMapper;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapter;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapterResolver;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderException;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderQueryResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderSubmitResult;
import cn.iocoder.yudao.module.ai.service.media.provider.worker.PythonWorkerMediaProviderAdapter;
import cn.iocoder.yudao.module.ai.service.media.provider.xiaomi.XiaomiMiMoTtsProviderAdapter;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.MEDIA_PROVIDER_CONFIG_MISSING;

class AiMediaTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiMediaTaskServiceImpl service;

    @Mock
    private AiMediaTaskMapper mediaTaskMapper;
    @Mock
    private AiVoiceProfileMapper voiceProfileMapper;
    @Mock
    private AiModelService modelService;
    @Mock
    private AiMediaProviderAdapterResolver providerAdapterResolver;
    @Mock
    private FileApi fileApi;
    @Mock
    private AiRemoteResourceService remoteResourceService;
    @Mock
    private AiMediaProviderAdapter providerAdapter;
    @Mock
    private PythonWorkerMediaProviderAdapter pythonWorkerMediaProviderAdapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ttsEnable", true);
        ReflectionTestUtils.setField(service, "ttsUserDailyLimit", 100);
        ReflectionTestUtils.setField(service, "videoEnable", true);
        ReflectionTestUtils.setField(service, "videoUserDailyLimit", 20);
        ReflectionTestUtils.setField(service, "videoTenantDailyLimit", 200);
        ReflectionTestUtils.setField(service, "videoUserConcurrentLimit", 3);
        ReflectionTestUtils.setField(service, "videoMaxRetryCount", 2);
        ReflectionTestUtils.setField(service, "videoMaxDurationSeconds", 10);
        ReflectionTestUtils.setField(service, "videoMaxSourceBytes", 10_485_760L);
        ReflectionTestUtils.setField(service, "videoMaxResultBytes", 209_715_200L);
        ReflectionTestUtils.setField(service, "videoPollLimit", 20);
        ReflectionTestUtils.setField(service, "workerPollLimit", 20);
        ReflectionTestUtils.setField(service, "assetRetentionDays", 30);
        lenient().when(mediaTaskMapper.selectCountByTaskTypeAndCreateTime(any(), any(), any())).thenReturn(0L);
    }

    @Test
    void createTtsTask_success() {
        Long userId = 10L;
        AiMediaTtsCreateReqVO reqVO = new AiMediaTtsCreateReqVO()
                .setText("hello")
                .setVoice("voice")
                .setFormat("mp3");
        AiModelDO model = new AiModelDO()
                .setId(20L)
                .setType(AiModelTypeEnum.VOICE.getType())
                .setPlatform(AiPlatformEnum.XIAOMI_MIMO.getPlatform())
                .setModel(XiaomiMiMoTtsProviderAdapter.MODEL);
        when(modelService.getRequiredDefaultModel(AiModelTypeEnum.VOICE.getType())).thenReturn(model);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(userId), eq(AiMediaTaskTypeEnum.TTS.getType()), any(), any()))
                .thenReturn(0L);
        doAnswer(invocation -> {
            AiMediaTaskDO task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        }).when(mediaTaskMapper).insert(any(AiMediaTaskDO.class));

        TransactionSynchronizationManager.initSynchronization();
        Long taskId;
        List<TransactionSynchronization> synchronizations;
        try {
            taskId = service.createTtsTask(userId, reqVO);
            synchronizations = TransactionSynchronizationManager.getSynchronizations();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertEquals(100L, taskId);
        verify(mediaTaskMapper).insert(any(AiMediaTaskDO.class));
        verify(mediaTaskMapper, never()).updateById(any(AiMediaTaskDO.class));
        verify(providerAdapterResolver, never()).resolve(any(AiMediaTaskDO.class));
        assertEquals(1, synchronizations.size());
    }

    @Test
    void getProviderHealth_pythonWorkerOk() {
        ReflectionTestUtils.setField(service, "workerEnable", true);
        ReflectionTestUtils.setField(service, "workerBaseUrl", "http://127.0.0.1:8099");
        when(pythonWorkerMediaProviderAdapter.getHealthStatus()).thenReturn(Map.of(
                "ok", true,
                "status", "healthy",
                "mode", "mock"));

        Map<String, Object> result = service.getProviderHealth();

        Map<String, Object> worker = (Map<String, Object>) result.get(AiPlatformEnum.PYTHON_WORKER.getPlatform());
        assertEquals(true, worker.get("ok"));
        assertEquals("mock", worker.get("mode"));
        assertEquals(List.of("aiSiliconFlowVideoSyncJob", "aiPythonWorkerMediaSyncJob"), result.get("xxlJobHandlers"));
    }

    @Test
    void getProviderHealth_pythonWorkerFailed() {
        when(pythonWorkerMediaProviderAdapter.getHealthStatus())
                .thenThrow(new AiMediaProviderException("WORKER_NETWORK_ERROR", "调用 Python Worker 失败"));

        Map<String, Object> result = service.getProviderHealth();

        Map<String, Object> worker = (Map<String, Object>) result.get(AiPlatformEnum.PYTHON_WORKER.getPlatform());
        assertEquals(false, worker.get("ok"));
        assertEquals("WORKER_NETWORK_ERROR", worker.get("errorCode"));
    }

    @Test
    void createTask_tts_success() {
        Long userId = 10L;
        AiMediaTaskCreateReqVO reqVO = new AiMediaTaskCreateReqVO()
                .setTaskType(AiMediaTaskTypeEnum.TTS.getType())
                .setInputJson(Map.of("text", "hello", "voice", "voice", "format", "mp3"));
        AiModelDO model = new AiModelDO()
                .setId(20L)
                .setType(AiModelTypeEnum.VOICE.getType())
                .setPlatform(AiPlatformEnum.XIAOMI_MIMO.getPlatform())
                .setModel(XiaomiMiMoTtsProviderAdapter.MODEL);
        when(modelService.getRequiredDefaultModel(AiModelTypeEnum.VOICE.getType())).thenReturn(model);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(userId), eq(AiMediaTaskTypeEnum.TTS.getType()), any(), any()))
                .thenReturn(0L);
        doAnswer(invocation -> {
            AiMediaTaskDO task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        }).when(mediaTaskMapper).insert(any(AiMediaTaskDO.class));

        TransactionSynchronizationManager.initSynchronization();
        Long taskId;
        try {
            taskId = service.createTask(userId, reqVO);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertEquals(100L, taskId);
        verify(mediaTaskMapper).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_success() {
        Long userId = 10L;
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");
        AiModelDO model = new AiModelDO()
                .setId(20L)
                .setType(AiModelTypeEnum.VIDEO.getType())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModel("Wan-AI/Wan2.2-I2V-A14B")
                .setCapabilities(List.of("video", "image-to-video"));
        when(modelService.getRequiredDefaultModel(AiModelTypeEnum.VIDEO.getType())).thenReturn(model);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(userId), eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any(), any()))
                .thenReturn(0L);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndStatusIn(eq(userId), eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any()))
                .thenReturn(0L);
        doAnswer(invocation -> {
            AiMediaTaskDO task = invocation.getArgument(0);
            task.setId(200L);
            return 1;
        }).when(mediaTaskMapper).insert(any(AiMediaTaskDO.class));

        TransactionSynchronizationManager.initSynchronization();
        Long taskId;
        try {
            taskId = service.createImageToVideoTask(userId, reqVO);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertEquals(200L, taskId);
        ArgumentCaptor<AiMediaTaskDO> taskCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper).insert(taskCaptor.capture());
        AiMediaTaskDO task = taskCaptor.getValue();
        assertEquals(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(), task.getTaskType());
        assertEquals(AiMediaTaskStatusEnum.QUEUED.getStatus(), task.getStatus());
        assertEquals("https://file.local/source.png", task.getInputJson().get("sourceImageUrl"));
        assertTrue(task.getInputJson().containsKey("sourceAsset"));
        assertEquals(List.of("https://file.local/source.png"), task.getSourceFileIds());
    }

    @Test
    void createImageToVideoTask_pythonWorkerModel_success() {
        Long userId = 10L;
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setModelId(21L)
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");
        AiModelDO model = new AiModelDO()
                .setId(21L)
                .setType(AiModelTypeEnum.VIDEO.getType())
                .setPlatform(AiPlatformEnum.PYTHON_WORKER.getPlatform())
                .setModel("mock-i2v")
                .setCapabilities(List.of("video", "image-to-video"));
        when(modelService.validateModel(21L, AiModelTypeEnum.VIDEO)).thenReturn(model);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(userId),
                eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any(), any())).thenReturn(0L);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndStatusIn(eq(userId),
                eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any())).thenReturn(0L);
        doAnswer(invocation -> {
            AiMediaTaskDO task = invocation.getArgument(0);
            task.setId(201L);
            return 1;
        }).when(mediaTaskMapper).insert(any(AiMediaTaskDO.class));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertEquals(201L, service.createImageToVideoTask(userId, reqVO));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(mediaTaskMapper).insert(argThat((AiMediaTaskDO task) ->
                AiPlatformEnum.PYTHON_WORKER.getPlatform().equals(task.getPlatform())
                        && "mock-i2v".equals(task.getModel())));
    }

    @Test
    void createImageToVideoTask_projectFileOwner_success() {
        Long userId = 10L;
        String sourceUrl = "http://localhost:48080/admin-api/infra/file/1/get/source.png";
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl(sourceUrl)
                .setPrompt("cinematic")
                .setImageSize("1280x720");
        AiModelDO model = new AiModelDO()
                .setId(20L)
                .setType(AiModelTypeEnum.VIDEO.getType())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModel("Wan-AI/Wan2.2-I2V-A14B")
                .setCapabilities(List.of("video", "image-to-video"));
        when(fileApi.getFileByUrl(sourceUrl)).thenReturn(CommonResult.success(new FileRespDTO()
                .setUrl(sourceUrl)
                .setType("image/png")
                .setSize(2048L)
                .setCreator(String.valueOf(userId))));
        when(modelService.getRequiredDefaultModel(AiModelTypeEnum.VIDEO.getType())).thenReturn(model);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(userId), eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any(), any()))
                .thenReturn(0L);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndStatusIn(eq(userId), eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any()))
                .thenReturn(0L);
        doAnswer(invocation -> {
            AiMediaTaskDO task = invocation.getArgument(0);
            task.setId(200L);
            return 1;
        }).when(mediaTaskMapper).insert(any(AiMediaTaskDO.class));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertEquals(200L, service.createImageToVideoTask(userId, reqVO));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<AiMediaTaskDO> taskCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper).insert(taskCaptor.capture());
        Map<String, Object> inputJson = taskCaptor.getValue().getInputJson();
        assertEquals("image/png", inputJson.get("sourceMimeType"));
        assertEquals(2048L, inputJson.get("sourceSize"));
    }

    @Test
    void createImageToVideoTask_projectFileForbidden() {
        String sourceUrl = "http://localhost:48080/admin-api/infra/file/1/get/source.png";
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl(sourceUrl)
                .setPrompt("cinematic")
                .setImageSize("1280x720");
        when(fileApi.getFileByUrl(sourceUrl)).thenReturn(CommonResult.success(new FileRespDTO()
                .setUrl(sourceUrl)
                .setType("image/png")
                .setSize(2048L)
                .setCreator("11")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_SOURCE_FORBIDDEN.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_tenantQuotaExceeded() {
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(10L), eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any(), any()))
                .thenReturn(0L);
        when(mediaTaskMapper.selectCountByTaskTypeAndCreateTime(eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any(), any()))
                .thenReturn(200L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_TENANT_QUOTA_EXCEEDED.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_durationInvalid() {
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720")
                .setDuration(30);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_DURATION_INVALID.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_missingImage() {
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setPrompt("cinematic")
                .setImageSize("1280x720");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_SOURCE_REQUIRED.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_unsupportedImageType() {
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.gif")
                .setSourceMimeType("image/gif")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_SOURCE_INVALID.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_disabled() {
        ReflectionTestUtils.setField(service, "videoEnable", false);
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_DISABLED.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_userQuotaExceeded() {
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(10L), eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any(), any()))
                .thenReturn(20L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_QUOTA_EXCEEDED.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_concurrentExceeded() {
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(10L), eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any(), any()))
                .thenReturn(0L);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndStatusIn(eq(10L), eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), any()))
                .thenReturn(3L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_CONCURRENT_EXCEEDED.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_sourceTooLarge() {
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(10_485_761L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_SOURCE_INVALID.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createTask_textToVideoCapabilityNotEnabled() {
        AiMediaTaskCreateReqVO reqVO = new AiMediaTaskCreateReqVO()
                .setTaskType(AiMediaTaskTypeEnum.TEXT_TO_VIDEO.getType())
                .setInputJson(Map.of("prompt", "cinematic"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.createTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_TASK_TYPE_NOT_ENABLE.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void createTask_ttsWithVoiceProfile_success() {
        Long userId = 10L;
        AiMediaTaskCreateReqVO reqVO = new AiMediaTaskCreateReqVO()
                .setTaskType(AiMediaTaskTypeEnum.TTS.getType())
                .setInputJson(Map.of("voiceProfileId", 30L, "text", "hello", "format", "wav"));
        AiVoiceProfileDO profile = new AiVoiceProfileDO()
                .setId(30L)
                .setUserId(userId)
                .setSourceType(AiVoiceProfileSourceTypeEnum.CLONE.getType())
                .setModelId(20L)
                .setSampleFileUrl("https://file.local/sample.wav")
                .setSampleMimeType("audio/wav")
                .setConsentConfirmed(true)
                .setStatus(0);
        AiModelDO model = new AiModelDO()
                .setId(20L)
                .setType(AiModelTypeEnum.VOICE.getType())
                .setPlatform(AiPlatformEnum.XIAOMI_MIMO.getPlatform())
                .setModel(XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_CLONE);
        when(voiceProfileMapper.selectById(30L)).thenReturn(profile);
        when(modelService.validateModel(20L)).thenReturn(model);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(eq(userId), eq(AiMediaTaskTypeEnum.TTS.getType()), any(), any()))
                .thenReturn(0L);
        doAnswer(invocation -> {
            AiMediaTaskDO task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        }).when(mediaTaskMapper).insert(any(AiMediaTaskDO.class));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertEquals(100L, service.createTask(userId, reqVO));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<AiMediaTaskDO> taskCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper).insert(taskCaptor.capture());
        AiMediaTaskDO task = taskCaptor.getValue();
        assertEquals(XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_CLONE, task.getModel());
        assertEquals(AiVoiceProfileSourceTypeEnum.CLONE.getType(), task.getInputJson().get("voiceMode"));
        assertEquals("https://file.local/sample.wav", task.getInputJson().get("sampleFileUrl"));
        assertEquals(List.of("https://file.local/sample.wav"), task.getSourceFileIds());
    }

    @Test
    void createTask_ttsMissingText() {
        AiMediaTaskCreateReqVO reqVO = new AiMediaTaskCreateReqVO()
                .setTaskType(AiMediaTaskTypeEnum.TTS.getType())
                .setInputJson(Map.of("voice", "voice", "format", "mp3"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.createTask(10L, reqVO));
        assertEquals(MEDIA_PROVIDER_CONFIG_MISSING.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void executeTtsTask_success() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(100L)
                .setStatus(AiMediaTaskStatusEnum.CREATED.getStatus());
        when(providerAdapterResolver.resolve(any(AiMediaTaskDO.class))).thenReturn(providerAdapter);
        when(providerAdapter.execute(any(AiMediaTaskDO.class))).thenReturn(
                new AiMediaProviderResult("audio".getBytes(), "tts-100.mp3", "audio/mpeg", Map.of("ok", true)));
        when(fileApi.createFile(any(byte[].class), eq("tts-100.mp3"), eq("ai-media/tts"), eq("audio/mpeg")))
                .thenReturn("https://file.local/tts-100.mp3");

        service.executeTtsTask(task);

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper, times(2)).updateById(updateCaptor.capture());
        AiMediaTaskDO successUpdate = updateCaptor.getAllValues().get(1);
        assertEquals(AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), successUpdate.getStatus());
        assertEquals("https://file.local/tts-100.mp3", successUpdate.getOutputJson().get("url"));
    }

    @Test
    void submitImageToVideoTask_success() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setStatus(AiMediaTaskStatusEnum.QUEUED.getStatus());
        when(mediaTaskMapper.selectById(200L)).thenReturn(task);
        when(providerAdapterResolver.resolve(any(AiMediaTaskDO.class))).thenReturn(providerAdapter);
        when(providerAdapter.submit(any(AiMediaTaskDO.class))).thenReturn(
                new AiMediaProviderSubmitResult("sf-request-1", AiMediaTaskStatusEnum.QUEUED.getStatus(),
                        5, "submitted", Map.of("requestId", "sf-request-1")));

        service.submitImageToVideoTask(task);

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper, times(2)).updateById(updateCaptor.capture());
        AiMediaTaskDO submitted = updateCaptor.getAllValues().get(1);
        assertEquals("sf-request-1", submitted.getProviderTaskId());
        assertEquals(AiMediaTaskStatusEnum.QUEUED.getStatus(), submitted.getStatus());
        assertTrue(submitted.getOutputJson().containsKey("providerSubmit"));
    }

    @Test
    void submitImageToVideoTask_providerFailure() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setStatus(AiMediaTaskStatusEnum.QUEUED.getStatus());
        when(mediaTaskMapper.selectById(200L)).thenReturn(task, task);
        when(providerAdapterResolver.resolve(any(AiMediaTaskDO.class))).thenReturn(providerAdapter);
        when(providerAdapter.submit(any(AiMediaTaskDO.class)))
                .thenThrow(new AiMediaProviderException("HTTP_400", "bad request"));

        service.submitImageToVideoTask(task);

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper, times(2)).updateById(updateCaptor.capture());
        AiMediaTaskDO failed = updateCaptor.getAllValues().get(1);
        assertEquals(AiMediaTaskStatusEnum.FAILED.getStatus(), failed.getStatus());
        assertEquals("HTTP_400", failed.getErrorCode());
        assertEquals("bad request", failed.getErrorMessage());
    }

    @Test
    void getUserTaskAndRefresh_imageToVideo_refreshesStatus() {
        Long userId = 10L;
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(userId)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModel("Wan-AI/Wan2.2-I2V-A14B")
                .setModelId(20L)
                .setStatus(AiMediaTaskStatusEnum.QUEUED.getStatus())
                .setProviderTaskId("sf-request-1")
                .setOutputJson(Map.of());
        AiMediaTaskDO refreshed = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(userId)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus())
                .setProgress(50)
                .setStage("provider_running")
                .setProviderTaskId("sf-request-1");
        when(mediaTaskMapper.selectById(200L)).thenReturn(task, task, refreshed);
        when(providerAdapterResolver.resolve(task)).thenReturn(providerAdapter);
        when(providerAdapter.query(task)).thenReturn(new AiMediaProviderQueryResult(
                AiMediaTaskStatusEnum.RUNNING.getStatus(), 50, "provider_running",
                null, null, null, null, Map.of("status", "InProgress")));

        AiMediaTaskDO result = service.getUserTaskAndRefresh(200L, userId);

        assertEquals(AiMediaTaskStatusEnum.RUNNING.getStatus(), result.getStatus());
        assertEquals(50, result.getProgress());
        verify(mediaTaskMapper).updateById(argThat((AiMediaTaskDO update) ->
                AiMediaTaskStatusEnum.RUNNING.getStatus().equals(update.getStatus())
                        && "provider_running".equals(update.getStage())));
    }

    @Test
    void retryUserTask_failedImageToVideo_success() {
        Long userId = 10L;
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(userId)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setStatus(AiMediaTaskStatusEnum.FAILED.getStatus())
                .setOutputJson(Map.of("retryCount", 1, "cancelRequested", true));
        when(mediaTaskMapper.selectById(200L)).thenReturn(task);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.retryUserTask(200L, userId);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper).updateById(updateCaptor.capture());
        AiMediaTaskDO retryUpdate = updateCaptor.getValue();
        assertEquals(AiMediaTaskStatusEnum.RETRYING.getStatus(), retryUpdate.getStatus());
        assertEquals(2, retryUpdate.getOutputJson().get("retryCount"));
        assertFalse(retryUpdate.getOutputJson().containsKey("cancelRequested"));
    }

    @Test
    void retryUserTask_exceedMaxRetry() {
        Long userId = 10L;
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(userId)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setStatus(AiMediaTaskStatusEnum.FAILED.getStatus())
                .setOutputJson(Map.of("retryCount", 2));
        when(mediaTaskMapper.selectById(200L)).thenReturn(task);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.retryUserTask(200L, userId));
        assertEquals(ErrorCodeConstants.MEDIA_TASK_RETRY_EXCEEDED.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).updateById(any(AiMediaTaskDO.class));
    }

    @Test
    void cancelUserTask_beforeSubmit_cancelled() {
        Long userId = 10L;
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(userId)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setStatus(AiMediaTaskStatusEnum.QUEUED.getStatus())
                .setOutputJson(Map.of());
        when(mediaTaskMapper.selectById(200L)).thenReturn(task);

        service.cancelUserTask(200L, userId);

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper).updateById(updateCaptor.capture());
        AiMediaTaskDO cancelUpdate = updateCaptor.getValue();
        assertEquals(AiMediaTaskStatusEnum.CANCELLED.getStatus(), cancelUpdate.getStatus());
        assertEquals("cancelled_before_submit", cancelUpdate.getStage());
        assertEquals(true, cancelUpdate.getOutputJson().get("cancelRequested"));
    }

    @Test
    void cancelUserTask_submittedProviderUnsupported_cancelling() {
        Long userId = 10L;
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(userId)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus())
                .setProgress(50)
                .setProviderTaskId("sf-request-1")
                .setOutputJson(Map.of());
        when(mediaTaskMapper.selectById(200L)).thenReturn(task);
        when(providerAdapterResolver.resolve(task)).thenReturn(providerAdapter);
        doThrow(new UnsupportedOperationException("not supported")).when(providerAdapter).cancel(task);

        service.cancelUserTask(200L, userId);

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper).updateById(updateCaptor.capture());
        AiMediaTaskDO cancelUpdate = updateCaptor.getValue();
        assertEquals(AiMediaTaskStatusEnum.CANCELLING.getStatus(), cancelUpdate.getStatus());
        assertEquals(true, cancelUpdate.getOutputJson().get("cancelRequested"));
        assertEquals(true, cancelUpdate.getOutputJson().get("cancelProviderUnsupported"));
    }

    @Test
    void submitImageToVideoTask_cancelledBeforeSubmit_noProviderCall() {
        AiMediaTaskDO scheduledTask = new AiMediaTaskDO()
                .setId(200L)
                .setStatus(AiMediaTaskStatusEnum.QUEUED.getStatus());
        AiMediaTaskDO cancelledTask = new AiMediaTaskDO()
                .setId(200L)
                .setStatus(AiMediaTaskStatusEnum.CANCELLED.getStatus());
        when(mediaTaskMapper.selectById(200L)).thenReturn(cancelledTask);

        service.submitImageToVideoTask(scheduledTask);

        verify(providerAdapterResolver, never()).resolve(any(AiMediaTaskDO.class));
        verify(mediaTaskMapper, never()).updateById(any(AiMediaTaskDO.class));
    }

    @Test
    void createImageToVideoTask_textToVideoModel() {
        AiMediaImageToVideoCreateReqVO reqVO = new AiMediaImageToVideoCreateReqVO()
                .setSourceImageUrl("https://file.local/source.png")
                .setSourceMimeType("image/png")
                .setSourceSize(1024L)
                .setPrompt("cinematic")
                .setImageSize("1280x720");
        AiModelDO model = new AiModelDO()
                .setId(20L)
                .setType(AiModelTypeEnum.VIDEO.getType())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModel("Wan-AI/Wan2.2-T2V-A14B")
                .setCapabilities(List.of("video", "text-to-video"));
        when(modelService.getRequiredDefaultModel(AiModelTypeEnum.VIDEO.getType())).thenReturn(model);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createImageToVideoTask(10L, reqVO));
        assertEquals(ErrorCodeConstants.MEDIA_VIDEO_MODEL_CAPABILITY_UNSUPPORTED.getCode(), exception.getCode());
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void syncSiliconFlowImageToVideoTasks_successDownloadAndSave() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModel("Wan-AI/Wan2.2-I2V-A14B")
                .setModelId(20L)
                .setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus())
                .setProviderTaskId("sf-request-1")
                .setOutputJson(Map.of("providerSubmit", Map.of("requestId", "sf-request-1")));
        when(mediaTaskMapper.selectListByTaskTypeAndPlatformAndStatusInAndProviderTaskIdNotEmpty(any(), any(), any(), any()))
                .thenReturn(List.of(task));
        when(mediaTaskMapper.selectById(200L)).thenReturn(task);
        when(providerAdapterResolver.resolve(task)).thenReturn(providerAdapter);
        when(providerAdapter.query(task)).thenReturn(new AiMediaProviderQueryResult(
                AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), 100, "provider_succeeded",
                "https://provider.local/result.mp4", "video/mp4", null, null,
                Map.of("status", "Succeed")));
        when(remoteResourceService.download("https://provider.local/result.mp4", 209_715_200L))
                .thenReturn("video".getBytes());
        when(fileApi.createFile(any(byte[].class), eq("video-200.mp4"), eq("ai-media/video"), eq("video/mp4")))
                .thenReturn("https://file.local/video-200.mp4");

        assertEquals(1, service.syncSiliconFlowImageToVideoTasks());

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper).updateById(updateCaptor.capture());
        AiMediaTaskDO successUpdate = updateCaptor.getValue();
        assertEquals(AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), successUpdate.getStatus());
        assertEquals("https://file.local/video-200.mp4", successUpdate.getResultFileIds().get(0));
        assertTrue(successUpdate.getOutputJson().containsKey("resultAsset"));
    }

    @Test
    void syncPythonWorkerImageToVideoTasks_running() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(300L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.PYTHON_WORKER.getPlatform())
                .setModel("mock-i2v")
                .setStatus(AiMediaTaskStatusEnum.SUBMITTED.getStatus())
                .setProviderTaskId("worker-1")
                .setOutputJson(Map.of());
        when(mediaTaskMapper.selectListByTaskTypeAndPlatformAndStatusInAndProviderTaskIdNotEmpty(
                eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), eq(AiPlatformEnum.PYTHON_WORKER.getPlatform()),
                any(), eq(20))).thenReturn(List.of(task));
        when(mediaTaskMapper.selectById(300L)).thenReturn(task);
        when(providerAdapterResolver.resolve(task)).thenReturn(providerAdapter);
        when(providerAdapter.query(task)).thenReturn(new AiMediaProviderQueryResult(
                AiMediaTaskStatusEnum.RUNNING.getStatus(), 42, "rendering",
                null, null, null, null, Map.of("status", "running")));

        assertEquals(1, service.syncPythonWorkerImageToVideoTasks());

        verify(mediaTaskMapper).updateById(argThat((AiMediaTaskDO update) ->
                AiMediaTaskStatusEnum.RUNNING.getStatus().equals(update.getStatus())
                        && Integer.valueOf(42).equals(update.getProgress())
                        && "rendering".equals(update.getStage())));
    }

    @Test
    void syncPythonWorkerImageToVideoTasks_cancelRequestedProviderCancelled_marksCancelled() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(300L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.PYTHON_WORKER.getPlatform())
                .setModel("mock-i2v")
                .setStatus(AiMediaTaskStatusEnum.CANCELLING.getStatus())
                .setProviderTaskId("worker-1")
                .setOutputJson(Map.of("cancelRequested", true));
        when(mediaTaskMapper.selectListByTaskTypeAndPlatformAndStatusInAndProviderTaskIdNotEmpty(
                eq(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType()), eq(AiPlatformEnum.PYTHON_WORKER.getPlatform()),
                any(), eq(20))).thenReturn(List.of(task));
        when(mediaTaskMapper.selectById(300L)).thenReturn(task);
        when(providerAdapterResolver.resolve(task)).thenReturn(providerAdapter);
        when(providerAdapter.query(task)).thenReturn(new AiMediaProviderQueryResult(
                AiMediaTaskStatusEnum.CANCELLED.getStatus(), 100, "worker_cancelled",
                null, null, null, null, Map.of("status", "cancelled")));

        assertEquals(1, service.syncPythonWorkerImageToVideoTasks());

        verify(mediaTaskMapper).updateById(argThat((AiMediaTaskDO update) ->
                AiMediaTaskStatusEnum.CANCELLED.getStatus().equals(update.getStatus())
                        && Integer.valueOf(100).equals(update.getProgress())
                        && "cancelled_by_provider".equals(update.getStage())
                        && Map.of("status", "cancelled").equals(update.getOutputJson().get("providerStatus"))));
    }

    @Test
    void handlePythonWorkerCallback_running_recordsPayload() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(300L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.PYTHON_WORKER.getPlatform())
                .setModel("mock-i2v")
                .setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus())
                .setProviderTaskId("worker-1")
                .setOutputJson(Map.of());
        Map<String, Object> payload = Map.of("taskId", "300", "status", "running", "progress", 65);
        when(mediaTaskMapper.selectById(300L)).thenReturn(task);
        when(providerAdapterResolver.resolve(task)).thenReturn(providerAdapter);
        when(providerAdapter.verifyCallbackToken("worker-secret")).thenReturn(true);
        when(providerAdapter.handleCallback(task, payload)).thenReturn(new AiMediaProviderQueryResult(
                AiMediaTaskStatusEnum.RUNNING.getStatus(), 65, "encoding",
                null, null, null, null, payload));

        service.handlePythonWorkerCallback("worker-secret", payload);

        verify(mediaTaskMapper).updateById(argThat((AiMediaTaskDO update) ->
                AiMediaTaskStatusEnum.RUNNING.getStatus().equals(update.getStatus())
                        && Integer.valueOf(65).equals(update.getProgress())
                        && "RECEIVED".equals(update.getOutputJson().get("callbackStatus"))
                        && payload.equals(update.getOutputJson().get("workerCallback"))));
    }

    @Test
    void handlePythonWorkerCallback_terminalTask_ignoresRegression() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(300L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.PYTHON_WORKER.getPlatform())
                .setModel("mock-i2v")
                .setStatus(AiMediaTaskStatusEnum.SUCCEEDED.getStatus())
                .setProviderTaskId("worker-1");
        Map<String, Object> payload = Map.of("taskId", "300", "status", "running");
        when(mediaTaskMapper.selectById(300L)).thenReturn(task);
        when(providerAdapterResolver.resolve(task)).thenReturn(providerAdapter);
        when(providerAdapter.verifyCallbackToken("worker-secret")).thenReturn(true);
        when(providerAdapter.handleCallback(task, payload)).thenReturn(new AiMediaProviderQueryResult(
                AiMediaTaskStatusEnum.RUNNING.getStatus(), 20, "running",
                null, null, null, null, payload));

        service.handlePythonWorkerCallback("worker-secret", payload);

        verify(mediaTaskMapper, never()).updateById(any(AiMediaTaskDO.class));
    }

    @Test
    void handlePythonWorkerCallback_invalidToken() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(300L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.PYTHON_WORKER.getPlatform())
                .setModel("mock-i2v");
        when(mediaTaskMapper.selectById(300L)).thenReturn(task);
        when(providerAdapterResolver.resolve(task)).thenReturn(providerAdapter);

        AiMediaProviderException exception = assertThrows(AiMediaProviderException.class,
                () -> service.handlePythonWorkerCallback("bad-secret", Map.of("taskId", "300")));

        assertEquals("INVALID_CALLBACK_TOKEN", exception.getCode());
        verify(providerAdapter, never()).handleCallback(any(), any());
    }

    @Test
    void createTtsTask_fileSaveFail_markFailed() {
        AiMediaTtsCreateReqVO reqVO = new AiMediaTtsCreateReqVO()
                .setText("hello")
                .setVoice("voice")
                .setFormat("mp3");
        AiModelDO model = new AiModelDO()
                .setId(20L)
                .setType(AiModelTypeEnum.VOICE.getType())
                .setPlatform(AiPlatformEnum.XIAOMI_MIMO.getPlatform())
                .setModel(XiaomiMiMoTtsProviderAdapter.MODEL);
        when(modelService.getRequiredDefaultModel(AiModelTypeEnum.VOICE.getType())).thenReturn(model);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(any(), any(), any(), any())).thenReturn(0L);
        doAnswer(invocation -> {
            AiMediaTaskDO task = invocation.getArgument(0);
            task.setId(100L);
            return 1;
        }).when(mediaTaskMapper).insert(any(AiMediaTaskDO.class));
        when(providerAdapterResolver.resolve(any(AiMediaTaskDO.class))).thenReturn(providerAdapter);
        when(providerAdapter.execute(any(AiMediaTaskDO.class))).thenReturn(
                new AiMediaProviderResult("audio".getBytes(), "tts-100.mp3", "audio/mpeg", Map.of()));
        when(fileApi.createFile(any(byte[].class), any(), any(), any())).thenThrow(new RuntimeException("store down"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertEquals(100L, service.createTtsTask(10L, reqVO));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        service.executeTtsTask(new AiMediaTaskDO().setId(100L));

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper, times(2)).updateById(updateCaptor.capture());
        AiMediaTaskDO failedUpdate = updateCaptor.getAllValues().get(1);
        assertEquals(AiMediaTaskStatusEnum.FAILED.getStatus(), failedUpdate.getStatus());
        assertEquals("FILE_SAVE_FAIL", failedUpdate.getErrorCode());
        assertTrue(failedUpdate.getErrorMessage().contains("保存 TTS 音频文件失败"));
    }

    @Test
    void createTtsTask_quotaExceeded() {
        ReflectionTestUtils.setField(service, "ttsUserDailyLimit", 1);
        when(mediaTaskMapper.selectCountByUserAndTaskTypeAndCreateTime(any(), any(), any(), any())).thenReturn(1L);

        AiMediaTtsCreateReqVO reqVO = new AiMediaTtsCreateReqVO()
                .setText("hello")
                .setVoice("voice")
                .setFormat("mp3");

        assertThrows(ServiceException.class, () -> service.createTtsTask(10L, reqVO));
        verify(mediaTaskMapper, never()).insert(any(AiMediaTaskDO.class));
    }

    @Test
    void getUserTask_forbidden() {
        when(mediaTaskMapper.selectById(100L)).thenReturn(new AiMediaTaskDO().setId(100L).setUserId(11L));

        assertThrows(ServiceException.class, () -> service.getUserTask(100L, 10L));
    }

    @Test
    void mediaTaskDo_extendsTenantBaseDo() {
        assertTrue(TenantBaseDO.class.isAssignableFrom(AiMediaTaskDO.class));
    }

    @Test
    void getUserTaskAssets_successFiltersByOwnerAndBuildsMetadata() {
        LocalDateTime now = LocalDateTime.now();
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(10L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setStatus(AiMediaTaskStatusEnum.SUCCEEDED.getStatus())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModel("Wan-AI/Wan2.2-I2V-A14B")
                .setInputJson(Map.of(
                        "imageSize", "1280x720",
                        "sourceAsset", Map.of("assetType", "SOURCE_IMAGE", "url", "https://file.local/source.png",
                                "mimeType", "image/png", "size", 1024)))
                .setOutputJson(Map.of(
                        "resultAsset", Map.of("assetType", "RESULT_VIDEO", "url", "https://file.local/result.mp4",
                                "mimeType", "video/mp4", "size", 2048)));
        task.setTenantId(1L);
        task.setCreateTime(now.minusMinutes(10));
        task.setFinishTime(now);
        when(mediaTaskMapper.selectById(200L)).thenReturn(task);

        List<AiMediaAssetRespVO> assets = service.getUserTaskAssets(200L, 10L);

        assertEquals(2, assets.size());
        AiMediaAssetRespVO sourceAsset = assets.get(0);
        assertEquals("SOURCE_IMAGE", sourceAsset.getAssetType());
        assertEquals(10L, sourceAsset.getUserId());
        assertEquals(1L, sourceAsset.getTenantId());
        assertEquals("RETAINED_SOURCE", sourceAsset.getCleanupStatus());
        assertFalse(sourceAsset.getCleanupCandidate());

        AiMediaAssetRespVO resultAsset = assets.get(1);
        assertEquals("RESULT_VIDEO", resultAsset.getAssetType());
        assertEquals("https://file.local/result.mp4", resultAsset.getUrl());
        assertEquals(1280, resultAsset.getWidth());
        assertEquals(720, resultAsset.getHeight());
        assertEquals("ACTIVE", resultAsset.getCleanupStatus());
        assertNotNull(resultAsset.getExpireTime());
    }

    @Test
    void getUserTaskAssets_forbidden() {
        when(mediaTaskMapper.selectById(200L)).thenReturn(new AiMediaTaskDO().setId(200L).setUserId(11L));

        assertThrows(ServiceException.class, () -> service.getUserTaskAssets(200L, 10L));
    }

    @Test
    void getUserAssetPage_filtersResultVideoAndCleanupCandidate() {
        ReflectionTestUtils.setField(service, "assetRetentionDays", 1);
        LocalDateTime now = LocalDateTime.now();
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(10L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setStatus(AiMediaTaskStatusEnum.SUCCEEDED.getStatus())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModel("Wan-AI/Wan2.2-I2V-A14B")
                .setInputJson(Map.of(
                        "imageSize", "960x960",
                        "sourceAsset", Map.of("assetType", "SOURCE_IMAGE", "url", "https://file.local/source.png")))
                .setOutputJson(Map.of(
                        "resultAsset", Map.of("assetType", "RESULT_VIDEO", "url", "https://file.local/result.mp4",
                                "mimeType", "video/mp4", "size", 2048)));
        task.setTenantId(1L);
        task.setCreateTime(now.minusDays(3));
        task.setFinishTime(now.minusDays(2));
        when(mediaTaskMapper.selectListForAssetPage(any())).thenReturn(List.of(task));

        PageResult<AiMediaAssetRespVO> pageResult = service.getUserAssetPage(new AiMediaAssetPageReqVO()
                .setAssetType("RESULT_VIDEO")
                .setCleanupCandidate(true), 10L);

        assertEquals(1L, pageResult.getTotal());
        assertEquals("RESULT_VIDEO", pageResult.getList().get(0).getAssetType());
        assertTrue(pageResult.getList().get(0).getCleanupCandidate());
        verify(mediaTaskMapper).selectListForAssetPage(argThat(reqVO -> Long.valueOf(10L).equals(reqVO.getUserId())));
    }

    @Test
    void updateTaskFailed_recordsUsageRecord() {
        AiMediaTaskDO task = new AiMediaTaskDO()
                .setId(200L)
                .setUserId(10L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModel("Wan-AI/Wan2.2-I2V-A14B")
                .setInputJson(Map.of(
                        "duration", 5,
                        "sourceAsset", Map.of("assetType", "SOURCE_IMAGE", "size", 1024)));
        task.setTenantId(1L);
        task.setCreateTime(LocalDateTime.now().minusSeconds(10));
        when(mediaTaskMapper.selectById(200L)).thenReturn(task);

        service.updateTaskFailed(200L, "HTTP_429", "quota exceeded");

        ArgumentCaptor<AiMediaTaskDO> updateCaptor = ArgumentCaptor.forClass(AiMediaTaskDO.class);
        verify(mediaTaskMapper).updateById(updateCaptor.capture());
        Map<String, Object> usageRecord = (Map<String, Object>) updateCaptor.getValue().getOutputJson()
                .get("usageRecord");
        assertEquals(200L, usageRecord.get("taskId"));
        assertEquals(AiPlatformEnum.SILICON_FLOW.getPlatform(), usageRecord.get("provider"));
        assertEquals("Wan-AI/Wan2.2-I2V-A14B", usageRecord.get("model"));
        assertEquals(1024L, usageRecord.get("sourceSize"));
        assertEquals(5, usageRecord.get("duration"));
        assertEquals("HTTP_429", usageRecord.get("errorCode"));
    }

    @Test
    void createTask_unsupportedType() {
        AiMediaTaskCreateReqVO reqVO = new AiMediaTaskCreateReqVO()
                .setTaskType(AiMediaTaskTypeEnum.UNKNOWN.getType());

        assertThrows(ServiceException.class, () -> service.createTask(10L, reqVO));
    }

}
