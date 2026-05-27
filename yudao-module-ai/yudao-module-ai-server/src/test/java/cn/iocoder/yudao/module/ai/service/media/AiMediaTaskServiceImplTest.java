package cn.iocoder.yudao.module.ai.service.media;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.mysql.media.AiMediaTaskMapper;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapter;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderAdapterResolver;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderResult;
import cn.iocoder.yudao.module.ai.service.media.provider.xiaomi.XiaomiMiMoTtsProviderAdapter;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiMediaTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiMediaTaskServiceImpl service;

    @Mock
    private AiMediaTaskMapper mediaTaskMapper;
    @Mock
    private AiModelService modelService;
    @Mock
    private AiMediaProviderAdapterResolver providerAdapterResolver;
    @Mock
    private FileApi fileApi;
    @Mock
    private AiMediaProviderAdapter providerAdapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ttsEnable", true);
        ReflectionTestUtils.setField(service, "ttsUserDailyLimit", 100);
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
    void createTask_unsupportedType() {
        AiMediaTaskCreateReqVO reqVO = new AiMediaTaskCreateReqVO()
                .setTaskType(AiMediaTaskTypeEnum.UNKNOWN.getType());

        assertThrows(ServiceException.class, () -> service.createTask(10L, reqVO));
    }

}
