package cn.iocoder.yudao.module.ai.controller.admin.media;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetRespVO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiMediaTaskControllerTest {

    private final AiMediaTaskService mediaTaskService = mock(AiMediaTaskService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiMediaTaskController controller = new AiMediaTaskController();
        ReflectionTestUtils.setField(controller, "mediaTaskService", mediaTaskService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createTtsTask_success() throws Exception {
        when(mediaTaskService.createTtsTask(eq(10L), any())).thenReturn(100L);
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(post("/ai/media/task/tts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"text":"hello","voice":"voice","format":"mp3"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(100L));
        }
    }

    @Test
    void createImageToVideoTask_success() throws Exception {
        when(mediaTaskService.createImageToVideoTask(eq(10L), any())).thenReturn(200L);
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(post("/ai/media/task/image-to-video")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"sourceImageUrl":"https://file.local/source.png","sourceMimeType":"image/png","prompt":"cinematic","imageSize":"1280x720"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(200L));
        }
    }

    @Test
    void handlePythonWorkerCallback_success() throws Exception {
        mockMvc.perform(post("/ai/media/task/callback/python-worker")
                        .header("X-Worker-Token", "worker-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"taskId":"300","status":"running","progress":65}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(mediaTaskService).handlePythonWorkerCallback(eq("worker-secret"), argThat(payload ->
                "300".equals(payload.get("taskId")) && "running".equals(payload.get("status"))));
    }

    @Test
    void getMyTask_success() throws Exception {
        when(mediaTaskService.getUserTaskAndRefresh(100L, 10L)).thenReturn(new AiMediaTaskDO()
                .setId(100L)
                .setUserId(10L)
                .setStatus(AiMediaTaskStatusEnum.SUCCEEDED.getStatus())
                .setProgress(100));
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(get("/ai/media/task/get-my").param("id", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(100L))
                    .andExpect(jsonPath("$.data.status").value(AiMediaTaskStatusEnum.SUCCEEDED.getStatus()));
        }
    }

    @Test
    void getMyTask_withAsyncMetadata() throws Exception {
        when(mediaTaskService.getUserTaskAndRefresh(200L, 10L)).thenReturn(new AiMediaTaskDO()
                .setId(200L)
                .setUserId(10L)
                .setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus())
                .setInputJson(Map.of("sourceAsset", Map.of("url", "https://file.local/source.png")))
                .setOutputJson(Map.of(
                        "executionMode", "PROVIDER_ASYNC",
                        "retryCount", 1,
                        "cancelRequested", true,
                        "submittedTime", "2026-05-30T12:00:00",
                        "callbackStatus", "NONE")));
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(get("/ai/media/task/get-my").param("id", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.executionMode").value("PROVIDER_ASYNC"))
                    .andExpect(jsonPath("$.data.retryCount").value(1))
                    .andExpect(jsonPath("$.data.cancelRequested").value(true))
                    .andExpect(jsonPath("$.data.submittedTime").value("2026-05-30T12:00:00"))
                    .andExpect(jsonPath("$.data.callbackStatus").value("NONE"))
                    .andExpect(jsonPath("$.data.sourceAssets.url").value("https://file.local/source.png"));
        }
    }

    @Test
    void getMyTaskPage_success() throws Exception {
        when(mediaTaskService.getUserTaskPage(any(), eq(10L))).thenReturn(new PageResult<>(List.of(
                new AiMediaTaskDO()
                        .setId(200L)
                        .setUserId(10L)
                        .setStatus(AiMediaTaskStatusEnum.RUNNING.getStatus())
                        .setProgress(50)), 1L));
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(get("/ai/media/task/my-page")
                            .param("pageNo", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1L))
                    .andExpect(jsonPath("$.data.list[0].id").value(200L))
                    .andExpect(jsonPath("$.data.list[0].status").value(AiMediaTaskStatusEnum.RUNNING.getStatus()));
        }
    }

    @Test
    void retryMyTask_success() throws Exception {
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(post("/ai/media/task/retry-my").param("id", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(true));
        }
        verify(mediaTaskService).retryUserTask(200L, 10L);
    }

    @Test
    void cancelMyTask_success() throws Exception {
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(post("/ai/media/task/cancel-my").param("id", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(true));
        }
        verify(mediaTaskService).cancelUserTask(200L, 10L);
    }

    @Test
    void getProviderHealth_success() throws Exception {
        when(mediaTaskService.getProviderHealth()).thenReturn(Map.of(
                "PythonWorker", Map.of("ok", true, "status", "healthy")));

        mockMvc.perform(get("/ai/media/task/provider-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.PythonWorker.ok").value(true))
                .andExpect(jsonPath("$.data.PythonWorker.status").value("healthy"));
    }

    @Test
    void getMyAssetPage_success() throws Exception {
        when(mediaTaskService.getUserAssetPage(any(), eq(10L))).thenReturn(new PageResult<>(List.of(
                new AiMediaAssetRespVO()
                        .setTaskId(200L)
                        .setUserId(10L)
                        .setAssetType("RESULT_VIDEO")
                        .setUrl("https://file.local/result.mp4")
                        .setCleanupCandidate(false)), 1L));
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(get("/ai/media/task/my-assets")
                            .param("assetType", "RESULT_VIDEO")
                            .param("pageNo", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1L))
                    .andExpect(jsonPath("$.data.list[0].assetType").value("RESULT_VIDEO"))
                    .andExpect(jsonPath("$.data.list[0].url").value("https://file.local/result.mp4"));
        }
    }

    @Test
    void getMyTaskAssets_success() throws Exception {
        when(mediaTaskService.getUserTaskAssets(200L, 10L)).thenReturn(List.of(
                new AiMediaAssetRespVO()
                        .setTaskId(200L)
                        .setUserId(10L)
                        .setAssetType("SOURCE_IMAGE")
                        .setUrl("https://file.local/source.png")));
        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            mockMvc.perform(get("/ai/media/task/my-assets-by-task").param("taskId", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].assetType").value("SOURCE_IMAGE"))
                    .andExpect(jsonPath("$.data[0].url").value("https://file.local/source.png"));
        }
    }

}
