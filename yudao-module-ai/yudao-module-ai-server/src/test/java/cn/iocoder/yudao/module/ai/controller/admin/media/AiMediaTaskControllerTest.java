package cn.iocoder.yudao.module.ai.controller.admin.media;

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
    void getMyTask_success() throws Exception {
        when(mediaTaskService.getUserTask(100L, 10L)).thenReturn(new AiMediaTaskDO()
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

}
