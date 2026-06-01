package cn.iocoder.yudao.module.ai.service.media.provider.siliconflow;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderException;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderQueryResult;
import cn.iocoder.yudao.module.ai.service.media.provider.AiMediaProviderSubmitResult;
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
import static org.mockito.Mockito.when;

class SiliconFlowVideoProviderAdapterTest extends BaseMockitoUnitTest {

    @InjectMocks
    private SiliconFlowVideoProviderAdapter adapter;

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
        ReflectionTestUtils.setField(adapter, "maxSourceBytes", 10_485_760L);
        ReflectionTestUtils.setField(adapter, "requestTimeoutMillis", 120_000);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void supports_siliconFlowImageToVideo() {
        assertTrue(adapter.supports(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(),
                AiPlatformEnum.SILICON_FLOW.getPlatform(), SiliconFlowVideoProviderAdapter.MODEL_IMAGE_TO_VIDEO));
    }

    @Test
    void submit_success_mapsRequest() throws IOException {
        AtomicReference<String> authHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/submit", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"requestId":"sf-request-1"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaProviderSubmitResult result = adapter.submit(buildTask("http://127.0.0.1:" + server.getAddress().getPort()));

        assertEquals("Bearer test-key", authHeader.get());
        assertTrue(requestBody.get().contains("\"model\":\"Wan-AI/Wan2.2-I2V-A14B\""));
        assertTrue(requestBody.get().contains("\"image_size\":\"1280x720\""));
        assertTrue(requestBody.get().contains("\"image\":\"https://file.local/source.png\""));
        assertEquals("sf-request-1", result.getProviderTaskId());
        assertEquals(AiMediaTaskStatusEnum.QUEUED.getStatus(), result.getStatus());
    }

    @Test
    void submit_localProjectFileUrl_fallbackToBase64() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/submit", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"requestId":"sf-request-1"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        String localImageUrl = "http://127.0.0.1:" + server.getAddress().getPort()
                + "/admin-api/infra/file/1/get/source.png";
        when(remoteResourceService.isPrivateUrl(localImageUrl)).thenReturn(true);
        when(remoteResourceService.downloadPrivateUrl(localImageUrl, 10_485_760L))
                .thenReturn("image-bytes".getBytes(StandardCharsets.UTF_8));

        adapter.submit(buildTask("http://127.0.0.1:" + server.getAddress().getPort(), localImageUrl, "image/png"));

        assertTrue(requestBody.get().contains("\"image\":\"data:image/png;base64,aW1hZ2UtYnl0ZXM=\""));
    }

    @Test
    void query_success_mapsSucceed() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/status", exchange -> {
            byte[] response = """
                    {"status":"Succeed","results":{"videos":[{"url":"https://provider.local/result.mp4"}]}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaProviderQueryResult result = adapter.query(buildTask("http://127.0.0.1:" + server.getAddress().getPort())
                .setProviderTaskId("sf-request-1"));

        assertEquals(AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), result.getStatus());
        assertEquals("https://provider.local/result.mp4", result.getResultUrl());
    }

    @Test
    void query_running_mapsInProgress() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/status", exchange -> {
            byte[] response = """
                    {"status":"InProgress"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaProviderQueryResult result = adapter.query(buildTask("http://127.0.0.1:" + server.getAddress().getPort())
                .setProviderTaskId("sf-request-1"));

        assertEquals(AiMediaTaskStatusEnum.RUNNING.getStatus(), result.getStatus());
        assertEquals("provider_running", result.getStage());
    }

    @Test
    void query_failed_mapsProviderError() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/status", exchange -> {
            byte[] response = """
                    {"status":"Failed","message":"quota exceeded"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaProviderQueryResult result = adapter.query(buildTask("http://127.0.0.1:" + server.getAddress().getPort())
                .setProviderTaskId("sf-request-1"));

        assertEquals(AiMediaTaskStatusEnum.FAILED.getStatus(), result.getStatus());
        assertEquals("PROVIDER_FAILED", result.getErrorCode());
        assertEquals("quota exceeded", result.getErrorMessage());
    }

    @Test
    void query_failedCode_mapsProviderErrorCode() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/status", exchange -> {
            byte[] response = """
                    {"code":60011,"message":"Request failed: Unknown error.","data":null}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaProviderQueryResult result = adapter.query(buildTask("http://127.0.0.1:" + server.getAddress().getPort())
                .setProviderTaskId("sf-request-1"));

        assertEquals(AiMediaTaskStatusEnum.FAILED.getStatus(), result.getStatus());
        assertEquals("SILICONFLOW_60011", result.getErrorCode());
        assertEquals("SiliconFlow 请求失败：Request failed: Unknown error.", result.getErrorMessage());
    }

    @Test
    void submit_httpProviderError_parsesBody() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/submit", exchange -> {
            byte[] response = """
                    {"code":60011,"message":"Request failed: Unknown error.","data":null}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaProviderException exception = assertThrows(AiMediaProviderException.class,
                () -> adapter.submit(buildTask("http://127.0.0.1:" + server.getAddress().getPort())));

        assertEquals("SILICONFLOW_60011", exception.getCode());
        assertEquals("SiliconFlow 请求失败：Request failed: Unknown error.", exception.getMessage());
    }

    @Test
    void submit_malformedResponse() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/submit", exchange -> {
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaProviderException exception = assertThrows(AiMediaProviderException.class,
                () -> adapter.submit(buildTask("http://127.0.0.1:" + server.getAddress().getPort())));
        assertEquals("INVALID_RESPONSE", exception.getCode());
    }

    @Test
    void submit_timeout() throws IOException {
        ReflectionTestUtils.setField(adapter, "requestTimeoutMillis", 50);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/video/submit", exchange -> {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] response = """
                    {"requestId":"sf-request-1"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiMediaProviderException exception = assertThrows(AiMediaProviderException.class,
                () -> adapter.submit(buildTask("http://127.0.0.1:" + server.getAddress().getPort())));
        assertEquals("NETWORK_ERROR", exception.getCode());
    }

    private AiMediaTaskDO buildTask(String url) {
        return buildTask(url, "https://file.local/source.png", null);
    }

    private AiMediaTaskDO buildTask(String url, String sourceImageUrl, String sourceMimeType) {
        when(modelService.validateModel(20L)).thenReturn(new AiModelDO()
                .setId(20L)
                .setKeyId(30L)
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform()));
        when(apiKeyService.validateApiKey(30L)).thenReturn(new AiApiKeyDO()
                .setId(30L)
                .setApiKey("test-key")
                .setUrl(url)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));
        return new AiMediaTaskDO()
                .setId(100L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.SILICON_FLOW.getPlatform())
                .setModelId(20L)
                .setModel(SiliconFlowVideoProviderAdapter.MODEL_IMAGE_TO_VIDEO)
                .setInputJson(Map.of(
                        "prompt", "hello",
                        "sourceImageUrl", sourceImageUrl,
                        "sourceMimeType", sourceMimeType == null ? "" : sourceMimeType,
                        "imageSize", "1280x720",
                        "negativePrompt", "blur",
                        "seed", 123));
    }

}
