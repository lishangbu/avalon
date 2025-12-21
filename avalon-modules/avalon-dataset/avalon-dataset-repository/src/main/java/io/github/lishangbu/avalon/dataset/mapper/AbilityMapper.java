package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.dataset.entity.Ability;
import org.apache.ibatis.annotations.Mapper;

/**
 * 特性(Ability)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface AbilityMapper extends BaseMapper<Ability> {}
