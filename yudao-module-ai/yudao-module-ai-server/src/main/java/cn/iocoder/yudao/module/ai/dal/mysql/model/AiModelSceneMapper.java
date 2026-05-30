package cn.iocoder.yudao.module.ai.dal.mysql.model;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelSceneDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型场景默认配置 Mapper。
 *
 * @author codex
 */
@Mapper
public interface AiModelSceneMapper extends BaseMapperX<AiModelSceneDO> {

    default AiModelSceneDO selectFirstByScene(String scene) {
        return selectOne(new QueryWrapperX<AiModelSceneDO>()
                .eq("scene", scene)
                .eq("status", CommonStatusEnum.ENABLE.getStatus())
                .limitN(1)
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

}
