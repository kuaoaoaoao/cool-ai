package cn.iocoder.yudao.module.ai.dal.mysql.media;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ai.controller.admin.media.vo.voice.AiVoiceProfilePageReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.media.AiVoiceProfileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI 音色库 Mapper。
 *
 * @author codex
 */
@Mapper
public interface AiVoiceProfileMapper extends BaseMapperX<AiVoiceProfileDO> {

    default PageResult<AiVoiceProfileDO> selectPage(AiVoiceProfilePageReqVO reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AiVoiceProfileDO>()
                .eq(AiVoiceProfileDO::getUserId, userId)
                .likeIfPresent(AiVoiceProfileDO::getName, reqVO.getName())
                .eqIfPresent(AiVoiceProfileDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(AiVoiceProfileDO::getStatus, reqVO.getStatus())
                .orderByDesc(AiVoiceProfileDO::getCreateTime)
                .orderByDesc(AiVoiceProfileDO::getId));
    }

    default List<AiVoiceProfileDO> selectListByUserIdAndStatus(Long userId, Integer status) {
        return selectList(new LambdaQueryWrapperX<AiVoiceProfileDO>()
                .eq(AiVoiceProfileDO::getUserId, userId)
                .eqIfPresent(AiVoiceProfileDO::getStatus, status)
                .orderByDesc(AiVoiceProfileDO::getCreateTime)
                .orderByDesc(AiVoiceProfileDO::getId));
    }

}
