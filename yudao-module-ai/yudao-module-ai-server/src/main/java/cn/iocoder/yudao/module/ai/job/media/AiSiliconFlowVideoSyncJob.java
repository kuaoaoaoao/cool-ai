package cn.iocoder.yudao.module.ai.job.media;

import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SiliconFlow 视频任务同步 Job。
 */
@Component
@Slf4j
public class AiSiliconFlowVideoSyncJob {

    @Resource
    private AiMediaTaskService mediaTaskService;

    @XxlJob("aiSiliconFlowVideoSyncJob")
    public void execute(String param) {
        Integer count = mediaTaskService.syncSiliconFlowImageToVideoTasks();
        log.info("[execute][同步 SiliconFlow 图生视频 ({}) 个]", count);
    }

}
