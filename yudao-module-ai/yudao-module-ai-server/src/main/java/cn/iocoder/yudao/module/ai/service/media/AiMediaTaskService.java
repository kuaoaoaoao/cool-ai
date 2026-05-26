package cn.iocoder.yudao.module.ai.service.media;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import jakarta.validation.Valid;

public interface AiMediaTaskService {

    Long createTtsTask(Long userId, @Valid AiMediaTtsCreateReqVO reqVO);

    Long createTask(Long userId, @Valid AiMediaTaskCreateReqVO reqVO);

    AiMediaTaskDO getTask(Long id);

    AiMediaTaskDO getUserTask(Long id, Long userId);

    PageResult<AiMediaTaskDO> getTaskPage(AiMediaTaskPageReqVO pageReqVO);

    PageResult<AiMediaTaskDO> getUserTaskPage(AiMediaTaskPageReqVO pageReqVO, Long userId);

    void updateTaskRunning(Long id, String stage);

    void updateTaskSucceeded(Long id, String stage, String fileUrl, String mimeType, Object providerOutput);

    void updateTaskFailed(Long id, String errorCode, String errorMessage);

}
