package cn.iocoder.yudao.module.ai.framework.ai.core.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * AI 模型能力描述。
 *
 * @author kuao
 */
@Data
@Accessors(chain = true)
public class AiModelCapability {

    /**
     * 能力标签，例如 chat、vision、stream、tts。
     */
    private List<String> capabilities;

    /**
     * 输入模态，例如 text、image。
     */
    private List<String> inputModalities;

    /**
     * 输出模态，例如 text、image、audio。
     */
    private List<String> outputModalities;

    /**
     * 配置提示，展示给后台管理员。
     */
    private List<String> configTips;

}
