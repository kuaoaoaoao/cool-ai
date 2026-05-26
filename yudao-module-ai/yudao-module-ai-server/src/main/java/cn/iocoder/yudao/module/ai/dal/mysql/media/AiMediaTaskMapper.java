package cn.iocoder.yudao.module.ai.dal.mysql.media;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.AiMediaTaskPageReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiMediaTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

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
                .orderByDesc(AiMediaTaskDO::getId));
    }

    default PageResult<AiMediaTaskDO> selectPageByUser(AiMediaTaskPageReqVO reqVO, Long userId) {
        reqVO.setUserId(userId);
        return selectPage(reqVO);
    }

    default Long selectCountByUserAndTaskTypeAndCreateTime(Long userId, Integer taskType,
                                                           LocalDateTime beginTime, LocalDateTime endTime) {
        return selectCount(new LambdaQueryWrapperX<AiMediaTaskDO>()
                .eq(AiMediaTaskDO::getUserId, userId)
                .eq(AiMediaTaskDO::getTaskType, taskType)
                .between(AiMediaTaskDO::getCreateTime, beginTime, endTime));
    }

}
