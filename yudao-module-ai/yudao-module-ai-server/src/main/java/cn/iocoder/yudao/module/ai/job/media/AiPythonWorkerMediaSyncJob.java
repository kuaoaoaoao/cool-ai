package cn.iocoder.yudao.module.ai.job.media;

import cn.iocoder.yudao.module.ai.service.media.AiMediaTaskService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Python Worker 媒体任务同步 Job。
 */
@Component
@Slf4j
public class AiPythonWorkerMediaSyncJob {

    @Resource
    private AiMediaTaskService mediaTaskService;

    @XxlJob("aiPythonWorkerMediaSyncJob")
    public void execute(String param) {
        Integer count = mediaTaskService.syncPythonWorkerImageToVideoTasks();
        log.info("[execute][同步 Python Worker 图生视频 ({}) 个]", count);
    }

}
