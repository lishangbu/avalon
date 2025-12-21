package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.dataset.entity.Pokemon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宝可梦(Pokemon)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface PokemonMapper extends BaseMapper<Pokemon> {}
