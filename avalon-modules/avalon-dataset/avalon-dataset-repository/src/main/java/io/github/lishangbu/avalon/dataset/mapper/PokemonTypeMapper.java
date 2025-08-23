package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.PokemonType;
import java.util.Optional;

/**
 * 宝可梦属性(pokemon_type)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface PokemonTypeMapper {

  /**
   * 通过id查询单条宝可梦属性数据
   *
   * @param id 主键
   * @return 可选的宝可梦属性
   */
  Optional<PokemonType> selectById(Long id);

  /**
   * 新增宝可梦属性
   *
   * @param pokemonType 实例对象
   * @return 影响行数
   */
  int insert(PokemonType pokemonType);

  /**
   * 修改宝可梦属性
   *
   * @param pokemonType 实例对象
   * @return 影响行数
   */
  int updateById(PokemonType pokemonType);

  /**
   * 通过id删除宝可梦属性
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}
