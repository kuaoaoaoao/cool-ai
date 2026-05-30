package cn.iocoder.yudao.module.ai.framework.ai.core.model;

import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelCapabilitiesTest {

    @Test
    void infer_prefersConfiguredFieldsAndInfersMissingFields() {
        AiModelDO model = new AiModelDO()
                .setPlatform(AiPlatformEnum.TONG_YI.getPlatform())
                .setType(AiModelTypeEnum.VIDEO.getType())
                .setModel("Wan-AI/Wan2.2-I2V-A1")
                .setCapabilities(List.of("custom-capability", " image-to-video "));

        AiModelCapability capability = AiModelCapabilities.infer(model);

        assertIterableEquals(List.of("custom-capability", "image-to-video"), capability.getCapabilities());
        assertIterableEquals(List.of("image", "text"), capability.getInputModalities());
        assertIterableEquals(List.of("video"), capability.getOutputModalities());
    }

    @Test
    void infer_videoImageToVideoByModelName() {
        AiModelDO model = new AiModelDO()
                .setPlatform(AiPlatformEnum.TONG_YI.getPlatform())
                .setType(AiModelTypeEnum.VIDEO.getType())
                .setModel("Wan-AI/Wan2.2-I2V-A1");

        AiModelCapability capability = AiModelCapabilities.infer(model);

        assertTrue(capability.getCapabilities().contains("video"));
        assertTrue(capability.getCapabilities().contains("image-to-video"));
        assertIterableEquals(List.of("image", "text"), capability.getInputModalities());
        assertIterableEquals(List.of("video"), capability.getOutputModalities());
    }

    @Test
    void infer_rerank() {
        AiModelDO model = new AiModelDO()
                .setPlatform(AiPlatformEnum.TONG_YI.getPlatform())
                .setType(AiModelTypeEnum.RERANK.getType())
                .setModel("gte-rerank-v2");

        AiModelCapability capability = AiModelCapabilities.infer(model);

        assertIterableEquals(List.of("rerank"), capability.getCapabilities());
        assertIterableEquals(List.of("text"), capability.getInputModalities());
        assertIterableEquals(List.of("score"), capability.getOutputModalities());
    }

}
