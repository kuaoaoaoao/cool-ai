package cn.iocoder.yudao.module.ai.framework.ai.core.model.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.siliconflow.SiliconFlowImageApi;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.siliconflow.SiliconFlowImageModel;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.siliconflow.SiliconFlowImageOptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SiliconFlowImageModel} 集成测试
 */
public class SiliconFlowImageModelTests {

    private final SiliconFlowImageModel imageModel = new SiliconFlowImageModel(
            new SiliconFlowImageApi("sk-epsakfenqnyzoxhmbucsxlhkdqlcbnimslqoivkshalvdozz") // 密钥
    );

    @Test
    @Disabled
    public void testCall() {
        // 准备参数
        SiliconFlowImageOptions imageOptions = SiliconFlowImageOptions.builder()
                .model("Kwai-Kolors/Kolors")
                .build();
        ImagePrompt prompt = new ImagePrompt("万里长城", imageOptions);

        // 方法调用
        ImageResponse response = imageModel.call(prompt);
        // 打印结果
        System.out.println(response);
    }

    @Test
    void builderDefaultsAndImageSize_success() {
        SiliconFlowImageOptions options = SiliconFlowImageOptions.builder()
                .model("Kwai-Kolors/Kolors")
                .width(1024)
                .height(768)
                .build();

        assertEquals(1, options.getBatchSize());
        assertEquals(25, options.getNumInferenceSteps());
        assertEquals(0.75F, options.getGuidanceScale());
        assertNotNull(options.getSeed());
        assertEquals("1024x768", options.getImageSize());
    }

    @Test
    void imageRequestSerializeImageSize_success() throws Exception {
        SiliconFlowImageApi.SiliconflowImageRequest request = new SiliconFlowImageApi.SiliconflowImageRequest(
                "万里长城", "Kwai-Kolors/Kolors", "1024x768", 1, null, 123, 25, 0.75F, null);

        String json = new ObjectMapper().writeValueAsString(request);

        assertTrue(json.contains("\"image_size\":\"1024x768\""));
        assertFalse(json.contains("\"imageSize\""));
    }

}
