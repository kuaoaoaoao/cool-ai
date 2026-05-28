package cn.iocoder.yudao.module.ai.framework.ai.core.model.xiaomi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * 小米 MiMo {@link ChatModel} 实现类，基于 MiMo Chat Completions 接口。
 *
 * @author kuao
 */
@Slf4j
@RequiredArgsConstructor
public class XiaomiMiMoChatModel implements ChatModel {

    public static final String BASE_URL = "https://api.xiaomimimo.com/v1";
    public static final String COMPLETE_PATH = "/chat/completions";

    public static final String MODEL_PRO = "mimo-v2.5-pro";
    public static final String MODEL_OMNI = "mimo-v2.5";
    public static final String MODEL_OMNI_V2 = "mimo-v2-omni";
    public static final String MODEL_FLASH = "mimo-v2-flash";
    public static final String MODEL_DEFAULT = MODEL_OMNI;

    /**
     * MiMo 请求结构兼容 OpenAI Chat Completions，但鉴权 Header 为 api-key。
     */
    private final ChatModel chatModel;

    @Override
    public ChatResponse call(Prompt prompt) {
        return chatModel.call(prompt);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return chatModel.stream(prompt);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return chatModel.getDefaultOptions();
    }

}
