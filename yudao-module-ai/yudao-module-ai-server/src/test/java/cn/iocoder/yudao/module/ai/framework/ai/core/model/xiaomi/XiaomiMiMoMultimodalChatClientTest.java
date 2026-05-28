package cn.iocoder.yudao.module.ai.framework.ai.core.model.xiaomi;

import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.common.AiRemoteResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XiaomiMiMoMultimodalChatClientTest {

    private XiaomiMiMoMultimodalChatClient client;

    @BeforeEach
    void setUp() {
        client = new XiaomiMiMoMultimodalChatClient();
        ReflectionTestUtils.setField(client, "maxInlineBytes", 37_500_000L);
        ReflectionTestUtils.setField(client, "videoFps", 2);
        ReflectionTestUtils.setField(client, "videoMediaResolution", "default");
        ReflectionTestUtils.setField(client, "remoteResourceService", new AiRemoteResourceService() {
            @Override
            public byte[] download(String url, long maxBytes) {
                if (url.endsWith(".mp3")) {
                    return "audio".getBytes(StandardCharsets.UTF_8);
                }
                if (url.endsWith(".mp4")) {
                    return "video".getBytes(StandardCharsets.UTF_8);
                }
                return new byte[0];
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildRequest_withAudioAndVideoAttachments() {
        String audioUrl = "http://file.local/test.mp3";
        String videoUrl = "http://file.local/test.mp4";

        AiModelDO model = new AiModelDO()
                .setPlatform(AiPlatformEnum.XIAOMI_MIMO.getPlatform())
                .setModel(XiaomiMiMoChatModel.MODEL_OMNI);
        Prompt prompt = new Prompt(List.of(
                new SystemMessage("你是一个助手"),
                UserMessage.builder()
                        .text("总结这段音视频")
                        .metadata(Map.of(XiaomiMiMoMultimodalChatClient.CURRENT_USER_MESSAGE_METADATA_KEY, true))
                        .build()));

        Map<String, Object> request = client.buildRequest(model, prompt, List.of(audioUrl, videoUrl));

        assertEquals("mimo-v2.5", request.get("model"));
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("你是一个助手", messages.get(0).get("content"));
        List<Map<String, Object>> parts = (List<Map<String, Object>>) messages.get(1).get("content");
        assertEquals("text", parts.get(0).get("type"));
        assertEquals("input_audio", parts.get(1).get("type"));
        assertTrue(parts.get(1).toString().contains("data:audio/mpeg;base64,YXVkaW8="));
        assertEquals("video_url", parts.get(2).get("type"));
        assertTrue(parts.get(2).toString().contains("data:video/mp4;base64,dmlkZW8="));
        assertEquals(2, parts.get(2).get("fps"));
        assertEquals("default", parts.get(2).get("media_resolution"));
    }

    @Test
    void supports_onlyWhenXiaomiHasAudioOrVideo() {
        AiModelDO model = new AiModelDO()
                .setPlatform(AiPlatformEnum.XIAOMI_MIMO.getPlatform())
                .setModel(XiaomiMiMoChatModel.MODEL_OMNI);

        assertTrue(client.supports(model, List.of("http://file.local/test.wav")));
        assertTrue(client.supports(model, List.of("http://file.local/test.mov")));
    }

}
