package cn.iocoder.yudao.module.ai.dal.mysql.media;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaAssetPageReqVO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskPageReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * AI 媒体任务 Mapper。
 *
 * @author codex
 */
@Mapper
public interface AiMediaTaskMapper extends BaseMapperX<AiMediaTaskDO> {

    default PageResult<AiMediaTaskDO> selectPage(AiMediaTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AiMediaTaskDO>()
                .eqIfPresent(AiMediaTaskDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AiMediaTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(AiMediaTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AiMediaTaskDO::getPlatform, reqVO.getPlatform())
                .betweenIfPresent(AiMediaTaskDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AiMediaTaskDO::getCreateTime)
                .orderByDesc(AiMediaTaskDO::getId));
    }

    default PageResult<AiMediaTaskDO> selectPageByUser(AiMediaTaskPageReqVO reqVO, Long userId) {
        reqVO.setUserId(userId);
        return selectPage(reqVO, new LambdaQueryWrapperX<AiMediaTaskDO>()
                .eq(AiMediaTaskDO::getUserId, userId)
                .eqIfPresent(AiMediaTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(AiMediaTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AiMediaTaskDO::getPlatform, reqVO.getPlatform())
                .betweenIfPresent(AiMediaTaskDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AiMediaTaskDO::getCreateTime)
                .orderByDesc(AiMediaTaskDO::getId));
    }

    default Long selectCountByUserAndTaskTypeAndCreateTime(Long userId, Integer taskType,
                                                           LocalDateTime beginTime, LocalDateTime endTime) {
        return selectCount(new LambdaQueryWrapperX<AiMediaTaskDO>()
                .eq(AiMediaTaskDO::getUserId, userId)
                .eq(AiMediaTaskDO::getTaskType, taskType)
                .between(AiMediaTaskDO::getCreateTime, beginTime, endTime));
    }

    default Long selectCountByTaskTypeAndCreateTime(Integer taskType, LocalDateTime beginTime, LocalDateTime endTime) {
        return selectCount(new LambdaQueryWrapperX<AiMediaTaskDO>()
                .eq(AiMediaTaskDO::getTaskType, taskType)
                .between(AiMediaTaskDO::getCreateTime, beginTime, endTime));
    }

    default Long selectCountByUserAndTaskTypeAndStatusIn(Long userId, Integer taskType, Collection<Integer> statuses) {
        return selectCount(new LambdaQueryWrapperX<AiMediaTaskDO>()
                .eq(AiMediaTaskDO::getUserId, userId)
                .eq(AiMediaTaskDO::getTaskType, taskType)
                .in(AiMediaTaskDO::getStatus, statuses));
    }

    default List<AiMediaTaskDO> selectListByTaskTypeAndPlatformAndStatusInAndProviderTaskIdNotEmpty(
            Integer taskType, String platform, Collection<Integer> statuses, Integer limit) {
        return selectList(new LambdaQueryWrapperX<AiMediaTaskDO>()
                .eq(AiMediaTaskDO::getTaskType, taskType)
                .eq(AiMediaTaskDO::getPlatform, platform)
                .in(AiMediaTaskDO::getStatus, statuses)
                .isNotNull(AiMediaTaskDO::getProviderTaskId)
                .ne(AiMediaTaskDO::getProviderTaskId, "")
                .orderByAsc(true, AiMediaTaskDO::getCreateTime)
                .orderByAsc(true, AiMediaTaskDO::getId)
                .last("LIMIT " + limit));
    }

    default List<AiMediaTaskDO> selectListForAssetPage(AiMediaAssetPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<AiMediaTaskDO>()
                .eqIfPresent(AiMediaTaskDO::getId, reqVO.getTaskId())
                .eqIfPresent(AiMediaTaskDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AiMediaTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(AiMediaTaskDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AiMediaTaskDO::getPlatform, reqVO.getPlatform())
                .betweenIfPresent(AiMediaTaskDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AiMediaTaskDO::getCreateTime)
                .orderByDesc(AiMediaTaskDO::getId));
    }

}
