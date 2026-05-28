package cn.iocoder.yudao.module.ai.service.common;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRemoteResourceServiceTest {

    private AiRemoteResourceService service;

    @BeforeEach
    void setUp() {
        service = new AiRemoteResourceService();
        ReflectionTestUtils.setField(service, "allowPrivateUrl", false);
        ReflectionTestUtils.setField(service, "trustedHosts", "");
    }

    @Test
    void download_rejectsNonHttpUrl() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.download("file:///etc/passwd", 1024));

        assertTrue(exception.getMessage().contains("仅支持 http/https"));
    }

    @Test
    void download_rejectsLocalhostUrl() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.download("http://127.0.0.1/test.png", 1024));

        assertTrue(exception.getMessage().contains("不允许访问内网或本机地址"));
    }

    @Test
    void download_rejectsBlankUrl() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.download(" ", 1024));

        assertTrue(exception.getMessage().contains("文件 URL 不能为空"));
    }

    @Test
    void toAsciiUrl_encodesUnicodePath() throws Exception {
        URL url = AiRemoteResourceService.toAsciiUrl(URI.create(
                "http://127.0.0.1:48080/admin-api/infra/file/29/get/20260528/握手洗手.png"));

        assertEquals("http://127.0.0.1:48080/admin-api/infra/file/29/get/20260528/"
                + "%E6%8F%A1%E6%89%8B%E6%B4%97%E6%89%8B.png", url.toString());
    }

}
