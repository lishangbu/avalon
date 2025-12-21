package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 招式伤害类别(MoveDamageClass)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface MoveDamageClassMapper extends BaseMapper<MoveDamageClass> {}
