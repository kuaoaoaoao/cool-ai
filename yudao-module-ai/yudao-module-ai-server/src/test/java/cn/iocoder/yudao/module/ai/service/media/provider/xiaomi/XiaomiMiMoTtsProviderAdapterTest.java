package cn.iocoder.yudao.module.ai.service.media.provider.xiaomi;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiVoiceProfileSourceTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderException;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderResult;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class XiaomiMiMoTtsProviderAdapterTest extends BaseMockitoUnitTest {

    @InjectMocks
    private XiaomiMiMoTtsProviderAdapter adapter;

    @Mock
    private AiModelService modelService;
    @Mock
    private AiApiKeyService apiKeyService;
    @Mock
    private AiRemoteResourceService remoteResourceService;

    private HttpServer server;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "objectMapper", new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void supports_xiaomiTts() {
        assertTrue(adapter.supports(AiMediaTaskTypeEnum.TTS.getType(),
                AiPlatformEnum.XIAOMI_MIMO.getPlatform(), XiaomiMiMoTtsProviderAdapter.MODEL));
    }

    @Test
    void execute_success_mapsRequestAndDecodesAudio() throws IOException {
        AtomicReference<String> apiKeyHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            apiKeyHeader.set(exchange.getRequestHeaders().getFirst("api-key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"choices":[{"message":{"audio":{"data":"YXVkaW8="}}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaTaskDO task = buildTask("http://127.0.0.1:" + server.getAddress().getPort());
        AiMediaProviderResult result = adapter.execute(task);

        assertEquals("test-key", apiKeyHeader.get());
        assertTrue(requestBody.get().contains("\"model\":\"mimo-v2.5-tts\""));
        assertTrue(requestBody.get().contains("\"role\":\"assistant\""));
        assertTrue(requestBody.get().contains("\"role\":\"user\""));
        assertArrayEquals("audio".getBytes(StandardCharsets.UTF_8), result.getContent());
        assertEquals("audio/mpeg", result.getMimeType());
    }

    @Test
    void execute_cloneVoice_buildsDataUri() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"choices":[{"message":{"audio":{"data":"YXVkaW8="}}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        String sampleUrl = "http://file.local/sample.wav";
        when(remoteResourceService.download(eq(sampleUrl), eq(7_500_000L))).thenReturn("sample".getBytes(StandardCharsets.UTF_8));
        AiMediaTaskDO task = buildTask("http://127.0.0.1:" + server.getAddress().getPort())
                .setModel(XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_CLONE)
                .setInputJson(Map.of(
                        "text", "hello",
                        "voiceMode", AiVoiceProfileSourceTypeEnum.CLONE.getType(),
                        "sampleFileUrl", sampleUrl,
                        "sampleMimeType", "audio/wav",
                        "format", "wav"));

        AiMediaProviderResult result = adapter.execute(task);

        assertTrue(requestBody.get().contains("\"model\":\"mimo-v2.5-tts-voiceclone\""));
        assertTrue(requestBody.get().contains("data:audio/wav;base64,c2FtcGxl"));
        assertEquals("audio/wav", result.getMimeType());
    }

    @Test
    void execute_designVoice_buildsPreviewRequest() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"choices":[{"message":{"audio":{"data":"YXVkaW8="}}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaTaskDO task = buildTask("http://127.0.0.1:" + server.getAddress().getPort())
                .setModel(XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_DESIGN)
                .setInputJson(Map.of(
                        "text", "hello",
                        "voiceMode", AiVoiceProfileSourceTypeEnum.DESIGN.getType(),
                        "designPrompt", "年轻男性，声音清亮",
                        "format", "mp3"));

        adapter.execute(task);

        assertTrue(requestBody.get().contains("\"model\":\"mimo-v2.5-tts-voicedesign\""));
        assertTrue(requestBody.get().contains("\"content\":\"年轻男性，声音清亮\""));
        assertTrue(requestBody.get().contains("\"optimize_text_preview\":true"));
    }

    @Test
    void execute_success_whenConfiguredUrlContainsV1() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] response = """
                    {"choices":[{"message":{"audio":{"data":"YXVkaW8="}}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaTaskDO task = buildTask("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        AiMediaProviderResult result = adapter.execute(task);

        assertArrayEquals("audio".getBytes(StandardCharsets.UTF_8), result.getContent());
    }

    @Test
    void execute_missingApiKey() {
        AiMediaTaskDO task = buildTask("http://127.0.0.1:1");
        when(apiKeyService.validateApiKey(30L)).thenReturn(new AiApiKeyDO()
                .setId(30L)
                .setApiKey("")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));

        AiMediaProviderException exception = assertThrows(AiMediaProviderException.class, () -> adapter.execute(task));
        assertEquals("MISSING_API_KEY", exception.getCode());
    }

    private AiMediaTaskDO buildTask(String url) {
        when(modelService.validateModel(20L)).thenReturn(new AiModelDO().setId(20L).setKeyId(30L));
        when(apiKeyService.validateApiKey(30L)).thenReturn(new AiApiKeyDO()
                .setId(30L)
                .setApiKey("test-key")
                .setUrl(url)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));
        return new AiMediaTaskDO()
                .setId(100L)
                .setTaskType(AiMediaTaskTypeEnum.TTS.getType())
                .setPlatform(AiPlatformEnum.XIAOMI_MIMO.getPlatform())
                .setModelId(20L)
                .setModel(XiaomiMiMoTtsProviderAdapter.MODEL)
                .setInputJson(Map.of("text", "hello", "stylePrompt", "warm", "voice", "voice", "format", "mp3"));
    }

}
