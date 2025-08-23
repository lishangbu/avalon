package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.Pokemon;
import java.util.Optional;

/**
 * 宝可梦(pokemon)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface PokemonMapper {

  /**
   * 通过id查询单条宝可梦数据
   *
   * @param id 主键
   * @return 可选的宝可梦
   */
  Optional<Pokemon> selectById(Long id);

  /**
   * 新增宝可梦
   *
   * @param pokemon 实例对象
   * @return 影响行数
   */
  int insert(Pokemon pokemon);

  /**
   * 修改宝可梦
   *
   * @param pokemon 实例对象
   * @return 影响行数
   */
  int updateById(Pokemon pokemon);

  /**
   * 通过id删除宝可梦
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}
