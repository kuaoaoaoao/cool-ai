package cn.iocoder.yudao.module.ai.service.model;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ai.controller.admin.model.vo.apikey.AiApiKeySaveReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.mysql.model.AiApiKeyMapper;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiApiKeyServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiApiKeyServiceImpl service;

    @Mock
    private AiApiKeyMapper apiKeyMapper;

    @Test
    void updateApiKey_blankApiKeyKeepsOldValue() {
        when(apiKeyMapper.selectById(1L)).thenReturn(new AiApiKeyDO()
                .setId(1L)
                .setApiKey("old-secret")
                .setPlatform(AiPlatformEnum.OPENAI.getPlatform())
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));

        service.updateApiKey(new AiApiKeySaveReqVO()
                .setId(1L)
                .setName(" OpenAI ")
                .setApiKey(" ")
                .setPlatform(" OpenAI ")
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));

        ArgumentCaptor<AiApiKeyDO> captor = ArgumentCaptor.forClass(AiApiKeyDO.class);
        verify(apiKeyMapper).updateById(captor.capture());
        assertEquals("old-secret", captor.getValue().getApiKey());
        assertEquals("OpenAI", captor.getValue().getName());
        assertEquals(AiPlatformEnum.OPENAI.getPlatform(), captor.getValue().getPlatform());
    }

    @Test
    void createApiKey_setsDefaultXiaomiBaseUrl() {
        service.createApiKey(new AiApiKeySaveReqVO()
                .setName("MiMo")
                .setApiKey("secret")
                .setPlatform(AiPlatformEnum.XIAOMI_MIMO.getPlatform())
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));

        ArgumentCaptor<AiApiKeyDO> captor = ArgumentCaptor.forClass(AiApiKeyDO.class);
        verify(apiKeyMapper).insert(captor.capture());
        assertEquals("https://api.xiaomimimo.com/v1", captor.getValue().getUrl());
    }

}
