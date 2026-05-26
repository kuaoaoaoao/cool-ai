package cn.iocoder.yudao.module.ai.enums.media;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * AI 媒体任务状态枚举。
 *
 * @author codex
 */
@Getter
@RequiredArgsConstructor
public enum AiMediaTaskStatusEnum implements ArrayValuable<Integer> {

    CREATED(0, "已创建"),
    QUEUED(10, "排队中"),
    RUNNING(20, "进行中"),
    SUCCEEDED(30, "已完成"),
    FAILED(40, "已失败"),
    CANCELLED(50, "已取消"),
    RETRYING(60, "重试中");

    private final Integer status;
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(AiMediaTaskStatusEnum::getStatus)
            .toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
