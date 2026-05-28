package cn.iocoder.yudao.module.ai.service.model;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.apikey.AiApiKeyPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.apikey.AiApiKeySaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.mysql.model.AiApiKeyMapper;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.API_KEY_DISABLE;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.API_KEY_NOT_EXISTS;

/**
 * AI API 密钥 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class AiApiKeyServiceImpl implements AiApiKeyService {

    private static final String XIAOMI_MIMO_DEFAULT_BASE_URL = "https://api.xiaomimimo.com/v1";
    private static final String OLLAMA_DEFAULT_BASE_URL = "http://127.0.0.1:11434";
    private static final String MASKED_API_KEY = "******";

    private final ConcurrentMap<Long, String> apiKeyCacheVersions = new ConcurrentHashMap<>();

    @Resource
    private AiApiKeyMapper apiKeyMapper;

    @Override
    public Long createApiKey(AiApiKeySaveReqVO createReqVO) {
        // 校验
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(StrUtil.trim(createReqVO.getPlatform()));
        if (StrUtil.isBlank(createReqVO.getApiKey())) {
            throw exception(API_KEY_NOT_EXISTS);
        }

        // 插入
        AiApiKeyDO apiKey = BeanUtils.toBean(createReqVO, AiApiKeyDO.class);
        normalizeApiKey(apiKey, platform);
        apiKeyMapper.insert(apiKey);
        // 返回
        return apiKey.getId();
    }

    @Override
    public void updateApiKey(AiApiKeySaveReqVO updateReqVO) {
        // 校验存在
        AiApiKeyDO oldApiKey = validateApiKeyExists(updateReqVO.getId());
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(StrUtil.trim(updateReqVO.getPlatform()));

        // 更新
        AiApiKeyDO updateObj = BeanUtils.toBean(updateReqVO, AiApiKeyDO.class);
        if (StrUtil.isBlank(updateObj.getApiKey()) || Objects.equals(MASKED_API_KEY, updateObj.getApiKey())) {
            updateObj.setApiKey(oldApiKey.getApiKey());
        }
        normalizeApiKey(updateObj, platform);
        apiKeyMapper.updateById(updateObj);
        refreshCacheVersion(updateObj.getId());
    }

    private void normalizeApiKey(AiApiKeyDO apiKey, AiPlatformEnum platform) {
        apiKey.setName(StrUtil.trim(apiKey.getName()))
                .setApiKey(StrUtil.trim(apiKey.getApiKey()))
                .setPlatform(platform.getPlatform())
                .setUrl(normalizeUrl(platform, apiKey.getUrl()));
    }

    private String normalizeUrl(AiPlatformEnum platform, String url) {
        url = StrUtil.trim(url);
        if (StrUtil.isBlank(url)) {
            if (platform == AiPlatformEnum.XIAOMI_MIMO) {
                return XIAOMI_MIMO_DEFAULT_BASE_URL;
            }
            if (platform == AiPlatformEnum.OLLAMA) {
                return OLLAMA_DEFAULT_BASE_URL;
            }
            return null;
        }
        return StrUtil.removeSuffix(url, "/");
    }

    @Override
    public void deleteApiKey(Long id) {
        // 校验存在
        validateApiKeyExists(id);
        // 删除
        apiKeyMapper.deleteById(id);
        refreshCacheVersion(id);
    }

    private AiApiKeyDO validateApiKeyExists(Long id) {
        AiApiKeyDO apiKey = apiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw exception(API_KEY_NOT_EXISTS);
        }
        return apiKey;
    }

    @Override
    public AiApiKeyDO getApiKey(Long id) {
        return apiKeyMapper.selectById(id);
    }

    @Override
    public AiApiKeyDO validateApiKey(Long id) {
        AiApiKeyDO apiKey = validateApiKeyExists(id);
        if (CommonStatusEnum.isDisable(apiKey.getStatus())) {
            throw exception(API_KEY_DISABLE);
        }
        return apiKey;
    }

    @Override
    public PageResult<AiApiKeyDO> getApiKeyPage(AiApiKeyPageReqVO pageReqVO) {
        return apiKeyMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AiApiKeyDO> getApiKeyList() {
        return apiKeyMapper.selectList();
    }

    @Override
    public AiApiKeyDO getRequiredDefaultApiKey(String platform, Integer status) {
        AiApiKeyDO apiKey = apiKeyMapper.selectFirstByPlatformAndStatus(platform, status);
        if (apiKey == null) {
            throw exception(API_KEY_NOT_EXISTS);
        }
        return apiKey;
    }

    @Override
    public String getApiKeyCacheVersion(Long id) {
        AiApiKeyDO apiKey = validateApiKeyExists(id);
        return apiKeyCacheVersions.computeIfAbsent(id, key -> buildCacheVersion(apiKey));
    }

    private void refreshCacheVersion(Long id) {
        apiKeyCacheVersions.put(id, buildCacheVersion(LocalDateTime.now()));
    }

    private String buildCacheVersion(AiApiKeyDO apiKey) {
        return buildCacheVersion(apiKey.getUpdateTime() != null ? apiKey.getUpdateTime() : apiKey.getCreateTime());
    }

    private String buildCacheVersion(LocalDateTime time) {
        return time == null ? String.valueOf(System.nanoTime()) : String.valueOf(time.hashCode());
    }

}
