package cn.iocoder.yudao.module.ai.service.media.provider.worker;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskStatusEnum;
import cn.iocoder.yudao.module.ai.enums.media.AiMediaTaskTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
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

class PythonWorkerMediaProviderAdapterTest extends BaseMockitoUnitTest {

    @InjectMocks
    private PythonWorkerMediaProviderAdapter adapter;

    @Mock
    private AiModelService modelService;
    @Mock
    private AiApiKeyService apiKeyService;

    private HttpServer server;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(adapter, "enable", true);
        ReflectionTestUtils.setField(adapter, "authToken", "worker-secret");
        ReflectionTestUtils.setField(adapter, "callbackUrl", "https://app.local/callback/python-worker");
        ReflectionTestUtils.setField(adapter, "requestTimeoutMillis", 30_000);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void supports_enabledImageToVideoWorker() {
        assertTrue(adapter.supports(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(),
                AiPlatformEnum.PYTHON_WORKER.getPlatform(), "mock-i2v"));
        ReflectionTestUtils.setField(adapter, "enable", false);
        assertTrue(adapter.supports(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType(),
                AiPlatformEnum.PYTHON_WORKER.getPlatform(), "mock-i2v"));
    }

    @Test
    void healthCheck_success() throws IOException {
        startServer("/health", 200, """
                {"status":"healthy"}
                """);

        assertTrue(adapter.healthCheck());
        assertEquals("healthy", adapter.getHealthStatus().get("status"));
        assertTrue((Boolean) adapter.getHealthStatus().get("ok"));
    }

    @Test
    void submit_success_mapsEnvelope() throws IOException {
        AtomicReference<String> authHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/tasks", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"accepted":true,"workerTaskId":"worker-1"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        configureBaseUrl();

        AiMediaProviderSubmitResult result = adapter.submit(buildTask());

        assertEquals("Bearer worker-secret", authHeader.get());
        assertTrue(requestBody.get().contains("\"taskId\":\"100\""));
        assertTrue(requestBody.get().contains("\"taskType\":\"IMAGE_TO_VIDEO\""));
        assertTrue(requestBody.get().contains("\"sourceFiles\""));
        assertTrue(requestBody.get().contains("\"callbackUrl\":\"https://app.local/callback/python-worker\""));
        assertEquals("worker-1", result.getProviderTaskId());
        assertEquals(AiMediaTaskStatusEnum.SUBMITTED.getStatus(), result.getStatus());
    }

    @Test
    void submit_workerDisabled_fallsBackToApiKeyUrl() throws IOException {
        AtomicReference<String> authHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/tasks", exchange -> {
            authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = """
                    {"accepted":true,"workerTaskId":"worker-1"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        ReflectionTestUtils.setField(adapter, "enable", false);
        when(modelService.validateModel(1000L)).thenReturn(AiModelDO.builder()
                .id(1000L).keyId(2000L).platform(AiPlatformEnum.PYTHON_WORKER.getPlatform()).build());
        when(apiKeyService.validateApiKey(2000L)).thenReturn(AiApiKeyDO.builder()
                .id(2000L)
                .platform(AiPlatformEnum.PYTHON_WORKER.getPlatform())
                .apiKey("api-worker-secret")
                .url("http://127.0.0.1:" + server.getAddress().getPort())
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build());

        AiMediaProviderSubmitResult result = adapter.submit(buildTask());

        assertEquals("Bearer api-worker-secret", authHeader.get());
        assertEquals("worker-1", result.getProviderTaskId());
    }

    @Test
    void query_running_mapsProgress() throws IOException {
        startServer("/tasks/worker-1", 200, """
                {"status":"running","progress":42,"stage":"rendering"}
                """);

        AiMediaProviderQueryResult result = adapter.query(buildTask().setProviderTaskId("worker-1"));

        assertEquals(AiMediaTaskStatusEnum.RUNNING.getStatus(), result.getStatus());
        assertEquals(42, result.getProgress());
        assertEquals("rendering", result.getStage());
    }

    @Test
    void query_success_mapsResultUrl() throws IOException {
        startServer("/tasks/worker-1", 200, """
                {"status":"succeeded","outputs":[{"url":"https://worker.local/result.mp4","mimeType":"video/mp4"}]}
                """);

        AiMediaProviderQueryResult result = adapter.query(buildTask().setProviderTaskId("worker-1"));

        assertEquals(AiMediaTaskStatusEnum.SUCCEEDED.getStatus(), result.getStatus());
        assertEquals("https://worker.local/result.mp4", result.getResultUrl());
    }

    @Test
    void query_failed_mapsError() throws IOException {
        startServer("/tasks/worker-1", 200, """
                {"status":"failed","errorCode":"RENDER_FAIL","errorMessage":"render failed"}
                """);

        AiMediaProviderQueryResult result = adapter.query(buildTask().setProviderTaskId("worker-1"));

        assertEquals(AiMediaTaskStatusEnum.FAILED.getStatus(), result.getStatus());
        assertEquals("RENDER_FAIL", result.getErrorCode());
        assertEquals("render failed", result.getErrorMessage());
    }

    @Test
    void cancel_postsCancelRequest() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/tasks/worker-1/cancel", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        configureBaseUrl();

        adapter.cancel(buildTask().setProviderTaskId("worker-1"));

        assertEquals("{}", requestBody.get());
    }

    @Test
    void callback_verifiesTokenAndMapsStatus() {
        assertTrue(adapter.verifyCallbackToken("worker-secret"));
        assertFalse(adapter.verifyCallbackToken("bad-secret"));

        AiMediaProviderQueryResult result = adapter.handleCallback(buildTask(), Map.of(
                "taskId", "100",
                "status", "running",
                "progress", 65,
                "stage", "encoding"));

        assertEquals(AiMediaTaskStatusEnum.RUNNING.getStatus(), result.getStatus());
        assertEquals(65, result.getProgress());
    }

    @Test
    void submit_rejected() throws IOException {
        startServer("/tasks", 200, """
                {"accepted":false,"message":"queue full"}
                """);

        AiMediaProviderException exception = assertThrows(AiMediaProviderException.class,
                () -> adapter.submit(buildTask()));

        assertEquals("WORKER_REJECTED", exception.getCode());
    }

    @Test
    void query_successWithoutUrl_rejected() throws IOException {
        startServer("/tasks/worker-1", 200, """
                {"status":"succeeded","outputs":[]}
                """);

        AiMediaProviderException exception = assertThrows(AiMediaProviderException.class,
                () -> adapter.query(buildTask().setProviderTaskId("worker-1")));

        assertEquals("INVALID_RESPONSE", exception.getCode());
    }

    private void startServer(String path, int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> {
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        configureBaseUrl();
    }

    private void configureBaseUrl() {
        ReflectionTestUtils.setField(adapter, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
    }

    private AiMediaTaskDO buildTask() {
        return new AiMediaTaskDO()
                .setId(100L)
                .setUserId(10L)
                .setTaskType(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO.getType())
                .setPlatform(AiPlatformEnum.PYTHON_WORKER.getPlatform())
                .setModelId(1000L)
                .setModel("mock-i2v")
                .setInputJson(Map.of(
                        "prompt", "hello",
                        "sourceAsset", Map.of("url", "https://file.local/source.png", "mimeType", "image/png")));
    }

}
