package cn.iocoder.yudao.module.ai.service.media;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaImageToVideoCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskCreateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTtsCreateReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public interface AiMediaTaskService {

    Long createTtsTask(Long userId, @Valid AiMediaTtsCreateReqVO reqVO);

    Long createImageToVideoTask(Long userId, @Valid AiMediaImageToVideoCreateReqVO reqVO);

    Long createTask(Long userId, @Valid AiMediaTaskCreateReqVO reqVO);

    AiMediaTaskDO getTask(Long id);

    AiMediaTaskDO getUserTask(Long id, Long userId);

    AiMediaTaskDO getUserTaskAndRefresh(Long id, Long userId);

    void retryUserTask(Long id, Long userId);

    void cancelUserTask(Long id, Long userId);

    PageResult<AiMediaTaskDO> getTaskPage(AiMediaTaskPageReqVO pageReqVO);

    PageResult<AiMediaTaskDO> getUserTaskPage(AiMediaTaskPageReqVO pageReqVO, Long userId);

    PageResult<AiMediaAssetRespVO> getAssetPage(AiMediaAssetPageReqVO pageReqVO);

    PageResult<AiMediaAssetRespVO> getUserAssetPage(AiMediaAssetPageReqVO pageReqVO, Long userId);

    List<AiMediaAssetRespVO> getTaskAssets(Long taskId);

    List<AiMediaAssetRespVO> getUserTaskAssets(Long taskId, Long userId);

    Integer syncSiliconFlowImageToVideoTasks();

    Integer syncPythonWorkerImageToVideoTasks();

    Map<String, Object> getProviderHealth();

    void handlePythonWorkerCallback(String token, Map<String, Object> payload);

    void updateTaskRunning(Long id, String stage);

    void updateTaskSucceeded(Long id, String stage, String fileUrl, String mimeType, Object providerOutput);

    void updateTaskFailed(Long id, String errorCode, String errorMessage);

}
