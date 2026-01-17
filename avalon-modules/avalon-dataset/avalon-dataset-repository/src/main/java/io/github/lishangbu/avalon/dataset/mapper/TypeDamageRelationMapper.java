package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/// 属性克制关系数据访问层
///
/// 提供属性克制关系的 CRUD 操作，包含插入、按复合主键更新与删除、以及支持动态条件的查询方法
///
/// @author lishangbu
/// @since 2025/09/14
@Mapper
public interface TypeDamageRelationMapper {

  /// 插入属性克制关系记录
  ///
  /// 将新的属性克制关系数据插入到数据库中，基于复合主键（攻击方ID和防御方ID）确保唯一性
  ///
  /// @param typeDamageRelation 属性克制关系实体对象，不能为空
  /// @return 影响的行数，成功插入返回 1
  int insert(TypeDamageRelation typeDamageRelation);

  /// 根据复合主键更新属性克制关系
  ///
  /// 根据攻击方ID和防御方ID更新对应的伤害倍数，仅更新 multiplier 字段
  ///
  /// @param typeDamageRelation 属性克制关系实体对象，包含更新后的数据
  /// @return 影响的行数，成功更新返回 1，记录不存在返回 0
  int update(TypeDamageRelation typeDamageRelation);

  /// 根据复合主键删除属性克制关系
  ///
  /// @param attackingTypeId 攻击者属性 ID
  /// @param defendingTypeId 防御者属性 ID
  /// @return 影响的行数，成功删除返回 1，记录不存在返回 0
  int deleteByAttackingTypeIdAndDefendingTypeId(Long attackingTypeId, Long defendingTypeId);

  /// 根据复合主键查询属性克制关系
  /// @param attackingTypeId 攻击方属性 ID，不能为空
  /// @param defendingTypeId 防御方属性 ID，不能为空
  /// @return 包含查询结果的 Optional 对象，如果记录存在则返回实体对象，否则返回空 Optional
  Optional<TypeDamageRelation> selectByAttackingTypeIdAndDefendingTypeId(
      Long attackingTypeId, Long defendingTypeId);

  /// 分页查询属性克制关系列表（支持动态条件）
  ///
  /// 根据提供的查询条件和分页参数进行分页查询，支持按攻击方ID、防御方ID、伤害倍数等条件筛选
  /// @param page 分页参数，包含页码、每页大小等信息
  /// @param typeDamageRelation 查询条件对象，包含筛选条件
  /// @return 分页结果，包含符合条件的属性克制关系列表和分页信息
  IPage<TypeDamageRelation> selectList(
      @Param("page") Page<TypeDamageRelation> page,
      @Param("typeDamageRelation") TypeDamageRelation typeDamageRelation);

  /// 查询属性克制关系列表（支持动态条件）
  /// @param typeDamageRelation 查询条件对象，包含筛选条件
  /// @return 符合条件的属性克制关系列表
  List<TypeDamageRelation> selectList(
      @Param("typeDamageRelation") TypeDamageRelation typeDamageRelation);
}
