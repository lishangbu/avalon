package io.github.lishangbu.avalon.dataset.mapper;

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import java.util.Optional;

/**
 * 属性伤害关系(type_damage_relation)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface TypeDamageRelationMapper {

  /**
   * 通过attackerTypeInternalName查询单条属性伤害关系数据
   *
   * @param attackerTypeInternalName 主键
   * @return 可选的属性伤害关系
   */
  Optional<TypeDamageRelation> selectById(String attackerTypeInternalName);

  /**
   * 新增属性伤害关系
   *
   * @param typeDamageRelation 实例对象
   * @return 影响行数
   */
  int insert(TypeDamageRelation typeDamageRelation);

  /**
   * 修改属性伤害关系
   *
   * @param typeDamageRelation 实例对象
   * @return 影响行数
   */
  int updateById(TypeDamageRelation typeDamageRelation);

  /**
   * 通过attackerTypeInternalName删除属性伤害关系
   *
   * @param attackerTypeInternalName 主键
   * @return 影响行数
   */
  int deleteById(String attackerTypeInternalName);
}
