package io.github.lishangbu.avalon.admin.service.dataset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import java.util.Optional;

/// 属性克制关系服务接口
///
/// 提供对属性相互克制关系的增删改查和分页/列表查询功能
///
/// @author lishangbu
/// @since 2025/12/06
public interface TypeDamageRelationService {

  /// 根据条件分页查询属性克制关系
  ///
  /// @param page               分页参数
  /// @param typeDamageRelation 查询条件实体，非空字段将作为过滤条件
  /// @return 分页结果
  IPage<TypeDamageRelation> getTypeDamageRelationPage(
      Page<TypeDamageRelation> page, TypeDamageRelation typeDamageRelation);

  /// 新增属性克制关系
  ///
  /// @param typeDamageRelation 要保存的实体
  /// @return 保存后的实体
  TypeDamageRelation save(TypeDamageRelation typeDamageRelation);

  /// 根据复合主键删除属性克制关系
  ///
  /// @param attackingTypeId 攻击方类型内部名称
  /// @param defendingTypeId 防御方类型内部名称
  void removeByAttackingTypeIdAndDefendingTypeId(Long attackingTypeId, Long defendingTypeId);

  /// 更新属性克制关系
  /// @param entity 要更新的实体
  /// @return 更新后的实体
  TypeDamageRelation update(TypeDamageRelation entity);

  /// 根据复合主键查询属性克制关系
  /// @param attackingTypeId 攻击方属性ID
  /// @param defendingTypeId 防御方属性ID
  /// @return 包含结果的 Optional
  Optional<TypeDamageRelation> getByAttackingTypeIdAndDefendingTypeId(
      Long attackingTypeId, Long defendingTypeId);
}
