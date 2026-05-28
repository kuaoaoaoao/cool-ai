package cn.iocoder.yudao.module.ai.service.model;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.AiModelConnectionTestRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.model.AiModelPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.model.AiModelSaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.mysql.model.AiChatMapper;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiModelFactory;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.midjourney.api.MidjourneyApi;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.suno.api.SunoApi;
import cn.iocoder.yudao.module.ai.service.media.provider.xiaomi.XiaomiMiMoTtsProviderAdapter;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import com.agentsflex.llm.ollama.OllamaLlm;
import com.agentsflex.llm.ollama.OllamaLlmConfig;
import com.agentsflex.llm.qwen.QwenLlm;
import com.agentsflex.llm.qwen.QwenLlmConfig;
import dev.tinyflow.core.Tinyflow;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.*;

/**
 * AI 模型 Service 实现类
 *
 * @author fansili
 */
@Service
@Validated
public class AiModelServiceImpl implements AiModelService {

    private static final String TEST_TARGET_MODEL = "MODEL";
    private static final String TEST_TARGET_API_KEY = "API_KEY";
    private static final String TEST_PROMPT = "ping";
    private static final int TEST_MAX_TOKENS = 16;
    private static final String XIAOMI_MIMO_DEFAULT_VOICE = "mimo_default";
    private static final String XIAOMI_MIMO_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String XIAOMI_MIMO_CHAT_COMPLETIONS_SUFFIX = "/chat/completions";

    @Resource
    private AiApiKeyService apiKeyService;

    @Resource
    private AiChatMapper modelMapper;

    @Resource
    private AiModelFactory modelFactory;

    @Override
    public Long createModel(AiModelSaveReqVO createReqVO) {
        // 1. 校验
        validateModelSaveReqVO(createReqVO);

        // 2. 插入
        AiModelDO model = BeanUtils.toBean(createReqVO, AiModelDO.class);
        modelMapper.insert(model);
        return model.getId();
    }

    @Override
    public void updateModel(AiModelSaveReqVO updateReqVO) {
        // 1. 校验
        validateModelExists(updateReqVO.getId());
        validateModelSaveReqVO(updateReqVO);

        // 2. 更新
        AiModelDO updateObj = BeanUtils.toBean(updateReqVO, AiModelDO.class);
        modelMapper.updateById(updateObj);
    }

    @Override
    public void deleteModel(Long id) {
        // 校验存在
        validateModelExists(id);
        // 删除
        modelMapper.deleteById(id);
    }

    private AiModelDO validateModelExists(Long id) {
        AiModelDO model = modelMapper.selectById(id);
        if (model == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        return model;
    }

    private void validateModelSaveReqVO(AiModelSaveReqVO reqVO) {
        normalizeModel(reqVO);
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(reqVO.getPlatform());
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(reqVO.getKeyId());
        if (!Objects.equals(platform.getPlatform(), apiKey.getPlatform())) {
            throw exception(MODEL_PLATFORM_KEY_NOT_MATCH, platform.getPlatform(), apiKey.getPlatform());
        }
    }

    private void normalizeModel(AiModelSaveReqVO reqVO) {
        reqVO.setName(StrUtil.trim(reqVO.getName()));
        reqVO.setModel(StrUtil.trim(reqVO.getModel()));
        reqVO.setPlatform(StrUtil.trim(reqVO.getPlatform()));
    }

    @Override
    public AiModelDO getModel(Long id) {
        return modelMapper.selectById(id);
    }

    @Override
    public AiModelDO getRequiredDefaultModel(Integer type) {
        AiModelDO model = modelMapper.selectFirstByStatus(type, CommonStatusEnum.ENABLE.getStatus());
        if (model == null) {
            throw exception(MODEL_DEFAULT_NOT_EXISTS);
        }
        return model;
    }

    @Override
    public PageResult<AiModelDO> getModelPage(AiModelPageReqVO pageReqVO) {
        return modelMapper.selectPage(pageReqVO);
    }

    @Override
    public AiModelDO validateModel(Long id) {
        AiModelDO model = validateModelExists(id);
        if (CommonStatusEnum.isDisable(model.getStatus())) {
            throw exception(MODEL_DISABLE);
        }
        return model;
    }

    @Override
    public List<AiModelDO> getModelListByStatusAndType(Integer status, Integer type, String platform) {
        return modelMapper.selectListByStatusAndType(status, type, platform);
    }

    @Override
    public AiModelConnectionTestRespVO testModelConnection(Long id) {
        AiModelDO model = validateModelExists(id);
        return doTestModelConnection(model, TEST_TARGET_MODEL);
    }

    @Override
    public AiModelConnectionTestRespVO testApiKeyConnection(Long keyId) {
        AiApiKeyDO apiKey = getApiKeyRequired(keyId);
        List<AiModelDO> models = modelMapper.selectListByKeyIdAndStatus(keyId, CommonStatusEnum.ENABLE.getStatus());
        AiModelDO model = models.stream()
                .filter(this::isTestableModel)
                .min(Comparator.comparingInt(this::getTestPriority)
                        .thenComparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
                .orElse(null);
        if (model == null && isStandaloneApiKeyTestable(apiKey)) {
            return doTestApiKeyOnlyConnection(apiKey);
        }
        if (model == null) {
            return buildBaseTestResp(TEST_TARGET_API_KEY, null, apiKey)
                    .setSuccess(false)
                    .setErrorCode("NO_TESTABLE_MODEL")
                    .setMessage("连接测试需要该 API Key 下至少配置一个已启用的可测试模型")
                    .setDetail("请先新增 CHAT、EMBEDDING 或支持轻量测试的模型，并确认模型状态为开启");
        }
        return doTestModelConnection(model, TEST_TARGET_API_KEY);
    }

    private AiModelConnectionTestRespVO doTestApiKeyOnlyConnection(AiApiKeyDO apiKey) {
        AiModelConnectionTestRespVO respVO = buildBaseTestResp(TEST_TARGET_API_KEY, null, apiKey);
        long startMillis = System.currentTimeMillis();
        try {
            executeApiKeySmokeTest(apiKey);
            return respVO.setSuccess(true)
                    .setDurationMillis(System.currentTimeMillis() - startMillis)
                    .setMessage("连接测试成功");
        } catch (Exception ex) {
            return respVO.setSuccess(false)
                    .setDurationMillis(System.currentTimeMillis() - startMillis)
                    .setErrorCode(resolveTestErrorCode(ex))
                    .setMessage(resolveTestErrorMessage(ex))
                    .setDetail(StrUtil.maxLength(ExceptionUtil.getRootCauseMessage(ex), 480));
        }
    }

    private void executeApiKeySmokeTest(AiApiKeyDO apiKey) {
        if (AiPlatformEnum.SUNO.getPlatform().equals(apiKey.getPlatform())) {
            modelFactory.getOrCreateSunoApi(apiKey.getId(), apiKeyService.getApiKeyCacheVersion(apiKey.getId()),
                    apiKey.getApiKey(), apiKey.getUrl()).getLimitUsage();
            return;
        }
        if (AiPlatformEnum.MIDJOURNEY.getPlatform().equals(apiKey.getPlatform())) {
            modelFactory.getOrCreateMidjourneyApi(apiKey.getId(), apiKeyService.getApiKeyCacheVersion(apiKey.getId()),
                    apiKey.getApiKey(), apiKey.getUrl()).getTaskList(List.of("connection-test"));
            return;
        }
        throw new UnsupportedOperationException("该平台需要先配置模型后再测试连接");
    }

    private AiModelConnectionTestRespVO doTestModelConnection(AiModelDO model, String target) {
        AiModelConnectionTestRespVO respVO = buildBaseTestResp(target, model, null);
        long startMillis = System.currentTimeMillis();
        try {
            AiApiKeyDO apiKey = getModelApiKeyForTest(model);
            respVO = buildBaseTestResp(target, model, apiKey);
            executeConnectionSmokeTest(model, apiKey);
            return respVO.setSuccess(true)
                    .setDurationMillis(System.currentTimeMillis() - startMillis)
                    .setMessage(resolveTestSuccessMessage(model));
        } catch (Exception ex) {
            return respVO.setSuccess(false)
                    .setDurationMillis(System.currentTimeMillis() - startMillis)
                    .setErrorCode(resolveTestErrorCode(ex))
                    .setMessage(resolveTestErrorMessage(ex))
                    .setDetail(StrUtil.maxLength(ExceptionUtil.getRootCauseMessage(ex), 480));
        }
    }

    private void executeConnectionSmokeTest(AiModelDO model, AiApiKeyDO apiKey) {
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        if (AiModelTypeEnum.CHAT.getType().equals(model.getType())) {
            ChatModel chatModel = modelFactory.getOrCreateChatModel(platform, apiKey.getId(),
                    apiKeyService.getApiKeyCacheVersion(apiKey.getId()), apiKey.getApiKey(), apiKey.getUrl());
            Prompt prompt = new Prompt(TEST_PROMPT, AiUtils.buildChatOptions(platform, model.getModel(),
                    model.getTemperature() != null ? model.getTemperature() : 0.1,
                    model.getMaxTokens() != null ? Math.min(model.getMaxTokens(), TEST_MAX_TOKENS) : TEST_MAX_TOKENS));
            ChatResponse response = chatModel.call(prompt);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new IllegalStateException("模型没有返回对话结果");
            }
            return;
        }
        if (AiModelTypeEnum.EMBEDDING.getType().equals(model.getType())) {
            EmbeddingModel embeddingModel = modelFactory.getOrCreateEmbeddingModel(platform, apiKey.getId(),
                    apiKeyService.getApiKeyCacheVersion(apiKey.getId()), apiKey.getApiKey(), apiKey.getUrl(), model.getModel());
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(TEST_PROMPT));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null
                    || response.getResult().getOutput().length == 0) {
                throw new IllegalStateException("模型没有返回向量结果");
            }
            return;
        }
        if (AiModelTypeEnum.IMAGE.getType().equals(model.getType())) {
            if (AiPlatformEnum.MIDJOURNEY.getPlatform().equals(model.getPlatform())) {
                MidjourneyApi midjourneyApi = modelFactory.getOrCreateMidjourneyApi(apiKey.getId(),
                        apiKeyService.getApiKeyCacheVersion(apiKey.getId()), apiKey.getApiKey(), apiKey.getUrl());
                midjourneyApi.getTaskList(List.of("connection-test"));
                return;
            }
            modelFactory.getOrCreateImageModel(platform, apiKey.getId(),
                    apiKeyService.getApiKeyCacheVersion(apiKey.getId()), apiKey.getApiKey(), apiKey.getUrl());
            return;
        }
        if (AiModelTypeEnum.VOICE.getType().equals(model.getType())
                && AiPlatformEnum.XIAOMI_MIMO.getPlatform().equals(model.getPlatform())) {
            testXiaomiMiMoTts(model, apiKey);
            return;
        }
        if (AiModelTypeEnum.VOICE.getType().equals(model.getType())) {
            throw new UnsupportedOperationException("该语音模型需要上传音频样本后才能完成连接测试，当前通用测试暂不支持 ASR/音色克隆类模型");
        }
        if (AiPlatformEnum.SUNO.getPlatform().equals(model.getPlatform())) {
            SunoApi sunoApi = modelFactory.getOrCreateSunoApi(apiKey.getId(),
                    apiKeyService.getApiKeyCacheVersion(apiKey.getId()), apiKey.getApiKey(), apiKey.getUrl());
            sunoApi.getLimitUsage();
            return;
        }
        throw new UnsupportedOperationException("暂不支持该模型类型的连接测试");
    }

    private String resolveTestSuccessMessage(AiModelDO model) {
        if (AiModelTypeEnum.IMAGE.getType().equals(model.getType())
                && !AiPlatformEnum.MIDJOURNEY.getPlatform().equals(model.getPlatform())) {
            return "连接配置校验成功，已跳过真实绘图以避免消耗额度";
        }
        return "连接测试成功";
    }

    private boolean isTestableModel(AiModelDO model) {
        return AiModelTypeEnum.CHAT.getType().equals(model.getType())
                || AiModelTypeEnum.EMBEDDING.getType().equals(model.getType())
                || AiModelTypeEnum.IMAGE.getType().equals(model.getType())
                || isXiaomiMiMoTtsConnectionTestable(model)
                || AiPlatformEnum.SUNO.getPlatform().equals(model.getPlatform());
    }

    private boolean isXiaomiMiMoTtsConnectionTestable(AiModelDO model) {
        return AiModelTypeEnum.VOICE.getType().equals(model.getType())
                && AiPlatformEnum.XIAOMI_MIMO.getPlatform().equals(model.getPlatform())
                && !XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_CLONE.equals(model.getModel());
    }

    private boolean isStandaloneApiKeyTestable(AiApiKeyDO apiKey) {
        return AiPlatformEnum.SUNO.getPlatform().equals(apiKey.getPlatform())
                || AiPlatformEnum.MIDJOURNEY.getPlatform().equals(apiKey.getPlatform());
    }

    private int getTestPriority(AiModelDO model) {
        if (AiModelTypeEnum.CHAT.getType().equals(model.getType())) {
            return 10;
        }
        if (AiModelTypeEnum.EMBEDDING.getType().equals(model.getType())) {
            return 20;
        }
        if (AiPlatformEnum.SUNO.getPlatform().equals(model.getPlatform())
                || AiPlatformEnum.MIDJOURNEY.getPlatform().equals(model.getPlatform())) {
            return 30;
        }
        if (AiModelTypeEnum.VOICE.getType().equals(model.getType())) {
            return 35;
        }
        if (AiModelTypeEnum.IMAGE.getType().equals(model.getType())) {
            return 40;
        }
        return 100;
    }

    private void testXiaomiMiMoTts(AiModelDO model, AiApiKeyDO apiKey) {
        if (XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_CLONE.equals(model.getModel())) {
            throw new UnsupportedOperationException("小米 MiMo 音色克隆模型要求 audio.voice 传入声音样本 DataURL，请在音色库选择样本后发起实际克隆测试");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model.getModel());
        request.put("messages", buildXiaomiMiMoTtsTestMessages(model));
        request.put("stream", false);
        request.put("audio", buildXiaomiMiMoTtsTestAudio(model));
        try (HttpResponse response = HttpRequest.post(buildXiaomiMiMoChatUrl(apiKey.getUrl()))
                .header("api-key", apiKey.getApiKey())
                .header("Content-Type", "application/json")
                .body(JsonUtils.toJsonString(request))
                .timeout(30_000)
                .execute()) {
            if (response.getStatus() < HttpStatus.HTTP_OK || response.getStatus() >= HttpStatus.HTTP_MULT_CHOICE) {
                throw new IllegalStateException("HTTP " + response.getStatus() + "：" + StrUtil.maxLength(response.body(), 480));
            }
        }
    }

    private List<Map<String, String>> buildXiaomiMiMoTtsTestMessages(AiModelDO model) {
        if (XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_DESIGN.equals(model.getModel())) {
            return List.of(
                    Map.of("role", "user", "content", "自然、清晰、适合中文播报的年轻女声"),
                    Map.of("role", "assistant", "content", TEST_PROMPT));
        }
        return List.of(Map.of("role", "assistant", "content", TEST_PROMPT));
    }

    private Map<String, Object> buildXiaomiMiMoTtsTestAudio(AiModelDO model) {
        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("format", "mp3");
        if (XiaomiMiMoTtsProviderAdapter.MODEL_VOICE_DESIGN.equals(model.getModel())) {
            audio.put("optimize_text_preview", true);
        } else {
            audio.put("voice", XIAOMI_MIMO_DEFAULT_VOICE);
        }
        return audio;
    }

    private String buildXiaomiMiMoChatUrl(String configuredUrl) {
        String baseUrl = StrUtil.blankToDefault(configuredUrl, "https://api.xiaomimimo.com");
        baseUrl = StrUtil.removeSuffix(baseUrl, "/");
        if (StrUtil.endWith(baseUrl, XIAOMI_MIMO_CHAT_COMPLETIONS_PATH)
                || StrUtil.endWith(baseUrl, XIAOMI_MIMO_CHAT_COMPLETIONS_SUFFIX)) {
            return baseUrl;
        }
        if (StrUtil.endWith(baseUrl, "/v1")) {
            return baseUrl + XIAOMI_MIMO_CHAT_COMPLETIONS_SUFFIX;
        }
        return baseUrl + XIAOMI_MIMO_CHAT_COMPLETIONS_PATH;
    }

    private AiModelConnectionTestRespVO buildBaseTestResp(String target, AiModelDO model, AiApiKeyDO apiKey) {
        return new AiModelConnectionTestRespVO()
                .setTarget(target)
                .setModelId(model != null ? model.getId() : null)
                .setKeyId(apiKey != null ? apiKey.getId() : model != null ? model.getKeyId() : null)
                .setPlatform(model != null ? model.getPlatform() : apiKey != null ? apiKey.getPlatform() : null)
                .setType(model != null ? model.getType() : null)
                .setModel(model != null ? model.getModel() : null);
    }

    private String resolveTestErrorCode(Exception ex) {
        Throwable rootCause = ExceptionUtil.getRootCause(ex);
        String message = rootCause != null ? rootCause.getMessage() : ex.getMessage();
        message = StrUtil.nullToEmpty(message).toLowerCase();
        if (ex instanceof UnsupportedOperationException) {
            return "UNSUPPORTED_MODEL_TYPE";
        }
        if (ex instanceof ServiceException serviceException) {
            return String.valueOf(serviceException.getCode());
        }
        if (StrUtil.containsAny(message, "401", "403", "unauthorized", "forbidden", "invalid api key", "api key")) {
            return "AUTH_ERROR";
        }
        if (StrUtil.containsAny(message, "quota", "insufficient", "rate limit", "429")) {
            return "QUOTA_OR_RATE_LIMIT";
        }
        if (StrUtil.containsAny(message, "model", "deployment", "not found", "404")) {
            return "MODEL_ERROR";
        }
        if (StrUtil.containsAny(message, "timeout", "timed out")) {
            return "TIMEOUT";
        }
        if (StrUtil.containsAny(message, "connect", "connection", "unknown host", "dns")) {
            return "NETWORK_ERROR";
        }
        return "PROVIDER_ERROR";
    }

    private String resolveTestErrorMessage(Exception ex) {
        if (ex instanceof UnsupportedOperationException) {
            return ex.getMessage();
        }
        if (ex instanceof ServiceException serviceException) {
            return serviceException.getMessage();
        }
        return "连接测试失败，请检查平台、API URL、API Key、模型名、额度或网络";
    }

    // ========== 与 Spring AI 集成 ==========

    @Override
    public ChatModel getChatModel(Long id) {
        AiModelDO model = validateModel(id, AiModelTypeEnum.CHAT);
        AiApiKeyDO apiKey = validateModelApiKey(model);
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        return modelFactory.getOrCreateChatModel(platform, apiKey.getId(), apiKeyService.getApiKeyCacheVersion(apiKey.getId()),
                apiKey.getApiKey(), apiKey.getUrl());
    }

    @Override
    public ImageModel getImageModel(Long id) {
        AiModelDO model = validateModel(id, AiModelTypeEnum.IMAGE);
        AiApiKeyDO apiKey = validateModelApiKey(model);
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        return modelFactory.getOrCreateImageModel(platform, apiKey.getId(), apiKeyService.getApiKeyCacheVersion(apiKey.getId()),
                apiKey.getApiKey(), apiKey.getUrl());
    }

    @Override
    public MidjourneyApi getMidjourneyApi(Long id) {
        AiModelDO model = validateModel(id, AiModelTypeEnum.IMAGE);
        AiApiKeyDO apiKey = validateModelApiKey(model);
        return modelFactory.getOrCreateMidjourneyApi(apiKey.getId(), apiKeyService.getApiKeyCacheVersion(apiKey.getId()),
                apiKey.getApiKey(), apiKey.getUrl());
    }

    @Override
    public SunoApi getSunoApi() {
        AiApiKeyDO apiKey = apiKeyService.getRequiredDefaultApiKey(
                AiPlatformEnum.SUNO.getPlatform(), CommonStatusEnum.ENABLE.getStatus());
        return modelFactory.getOrCreateSunoApi(apiKey.getId(), apiKeyService.getApiKeyCacheVersion(apiKey.getId()),
                apiKey.getApiKey(), apiKey.getUrl());
    }

    @Override
    public SunoApi getSunoApi(Long id) {
        AiModelDO model = validateModel(id, AiModelTypeEnum.VOICE);
        if (!AiPlatformEnum.SUNO.getPlatform().equals(model.getPlatform())) {
            throw exception(MODEL_USE_TYPE_ERROR);
        }
        AiApiKeyDO apiKey = validateModelApiKey(model);
        return modelFactory.getOrCreateSunoApi(apiKey.getId(), apiKeyService.getApiKeyCacheVersion(apiKey.getId()),
                apiKey.getApiKey(), apiKey.getUrl());
    }

    @Override
    public VectorStore getOrCreateVectorStore(Long id, Map<String, Class<?>> metadataFields) {
        // 获取模型 + 密钥
        AiModelDO model = validateModel(id, AiModelTypeEnum.EMBEDDING);
        AiApiKeyDO apiKey = validateModelApiKey(model);
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());

        // 创建或获取 EmbeddingModel 对象
        EmbeddingModel embeddingModel = modelFactory.getOrCreateEmbeddingModel(
                platform, apiKey.getId(), apiKeyService.getApiKeyCacheVersion(apiKey.getId()),
                apiKey.getApiKey(), apiKey.getUrl(), model.getModel());

        // 创建或获取 VectorStore 对象
         return modelFactory.getOrCreateVectorStore(SimpleVectorStore.class, embeddingModel, metadataFields);
//         return modelFactory.getOrCreateVectorStore(QdrantVectorStore.class, embeddingModel, metadataFields);
//         return modelFactory.getOrCreateVectorStore(RedisVectorStore.class, embeddingModel, metadataFields);
//         return modelFactory.getOrCreateVectorStore(MilvusVectorStore.class, embeddingModel, metadataFields);
    }

    @Override
    public AiModelDO validateModel(Long id, AiModelTypeEnum type) {
        AiModelDO model = validateModel(id);
        if (!Objects.equals(model.getType(), type.getType())) {
            throw exception(MODEL_USE_TYPE_ERROR);
        }
        return model;
    }

    private AiApiKeyDO validateModelApiKey(AiModelDO model) {
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        if (!Objects.equals(model.getPlatform(), apiKey.getPlatform())) {
            throw exception(MODEL_PLATFORM_KEY_NOT_MATCH, model.getPlatform(), apiKey.getPlatform());
        }
        return apiKey;
    }

    private AiApiKeyDO getModelApiKeyForTest(AiModelDO model) {
        AiApiKeyDO apiKey = getApiKeyRequired(model.getKeyId());
        if (!Objects.equals(model.getPlatform(), apiKey.getPlatform())) {
            throw exception(MODEL_PLATFORM_KEY_NOT_MATCH, model.getPlatform(), apiKey.getPlatform());
        }
        return apiKey;
    }

    private AiApiKeyDO getApiKeyRequired(Long id) {
        AiApiKeyDO apiKey = apiKeyService.getApiKey(id);
        if (apiKey == null) {
            throw exception(API_KEY_NOT_EXISTS);
        }
        return apiKey;
    }

    // TODO @lesan：是不是返回 Llm 对象会好点哈？
    @Override
    public void getLLmProvider4Tinyflow(Tinyflow tinyflow, Long modelId) {
        AiModelDO model = validateModel(modelId);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(apiKey.getPlatform());
        switch (platform) {
            // TODO @lesan 考虑到未来不需要使用agents-flex 现在仅测试通义千问
            // TODO @lesan：【重要】是不是可以实现一个 SpringAiLlm，这样的话，内部全部用它就好了。只实现 chat 部分；这样，就把 flex 作为一个 agent 框架，内部调用，还是 spring ai 相关的。成本可能低一点？！
            case TONG_YI:
                QwenLlmConfig qwenLlmConfig = new QwenLlmConfig();
                qwenLlmConfig.setApiKey(apiKey.getApiKey());
                qwenLlmConfig.setModel(model.getModel());
                // TODO @lesan：这个有点奇怪。。。如果一个链式里，有多个模型，咋整呀。。。
                tinyflow.setLlmProvider(id -> new QwenLlm(qwenLlmConfig));
                break;
            case OLLAMA:
                OllamaLlmConfig ollamaLlmConfig = new OllamaLlmConfig();
                ollamaLlmConfig.setEndpoint(apiKey.getUrl());
                ollamaLlmConfig.setModel(model.getModel());
                tinyflow.setLlmProvider(id -> new OllamaLlm(ollamaLlmConfig));
                break;
        }
    }

}
