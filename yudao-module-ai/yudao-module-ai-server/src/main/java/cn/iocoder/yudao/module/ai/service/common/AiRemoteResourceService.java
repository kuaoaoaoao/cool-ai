package cn.iocoder.yudao.module.ai.service.common;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * 统一处理 AI 模块对远程资源的下载，避免 SSRF 和无上限内存占用。
 */
@Service
@Slf4j
public class AiRemoteResourceService {

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final int MAX_REDIRECTS = 3;

    @Value("${yudao.ai.download.allow-private-url:false}")
    private Boolean allowPrivateUrl;
    @Value("${yudao.ai.download.trusted-hosts:}")
    private String trustedHosts;

    public byte[] download(String url, long maxBytes) {
        if (StrUtil.isBlank(url)) {
            throw invalidParamException("文件 URL 不能为空");
        }
        if (maxBytes <= 0) {
            throw invalidParamException("文件大小限制配置不正确");
        }
        try {
            return download(URI.create(url.trim()), maxBytes, 0);
        } catch (IllegalArgumentException e) {
            throw invalidParamException("文件 URL 格式不正确");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[download][url({}) 下载失败：{}]", url, e.getMessage());
            throw invalidParamException("文件下载失败：{}", e.getMessage());
        }
    }

    private byte[] download(URI uri, long maxBytes, int redirectCount) throws Exception {
        validateUri(uri);
        URL target = toAsciiUrl(uri);
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Yudao-AI-Resource-Fetcher");
        try {
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                if (redirectCount >= MAX_REDIRECTS) {
                    throw invalidParamException("远程文件重定向次数过多");
                }
                String location = connection.getHeaderField("Location");
                if (StrUtil.isBlank(location)) {
                    throw invalidParamException("远程文件重定向地址为空");
                }
                return download(uri.resolve(location), maxBytes, redirectCount + 1);
            }
            if (status < 200 || status >= 300) {
                throw invalidParamException("远程文件响应状态异常：{}", status);
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > maxBytes) {
                throw invalidParamException("文件大小超过限制：{} bytes", maxBytes);
            }
            try (InputStream input = connection.getInputStream()) {
                return readLimited(input, maxBytes);
            }
        } finally {
            connection.disconnect();
        }
    }

    static URL toAsciiUrl(URI uri) throws Exception {
        return URI.create(uri.toASCIIString()).toURL();
    }

    private void validateUri(URI uri) throws Exception {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw invalidParamException("仅支持 http/https 文件 URL");
        }
        String host = uri.getHost();
        if (StrUtil.isBlank(host)) {
            throw invalidParamException("文件 URL Host 不能为空");
        }
        if (Boolean.TRUE.equals(allowPrivateUrl) || getTrustedHostSet().contains(host.toLowerCase(Locale.ROOT))) {
            return;
        }
        InetAddress[] addresses = InetAddress.getAllByName(host);
        for (InetAddress address : addresses) {
            if (isPrivateAddress(address)) {
                throw invalidParamException("不允许访问内网或本机地址：{}", host);
            }
        }
    }

    private byte[] readLimited(InputStream input, long maxBytes) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw invalidParamException("文件大小超过限制：{} bytes", maxBytes);
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private boolean isPrivateAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        if (address instanceof Inet6Address) {
            byte first = address.getAddress()[0];
            return (first & (byte) 0xfe) == (byte) 0xfc;
        }
        return false;
    }

    private Set<String> getTrustedHostSet() {
        if (StrUtil.isBlank(trustedHosts)) {
            return Set.of();
        }
        return Arrays.stream(trustedHosts.split(","))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .map(host -> host.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

}
