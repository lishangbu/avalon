package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

/**
 * 属性克制关系数据访问层
 *
 * <p>提供属性克制关系的 CRUD 操作 继承 MyBatis-Plus BaseMapper，自动获得基础的增删改查方法 包含 insert、updateById、deleteById 等方法
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface TypeDamageRelationMapper {

  /**
   * 插入属性克制关系记录
   *
   * <p>将新的属性克制关系数据插入到数据库中 基于复合主键（攻击方ID和防御方ID）确保唯一性
   *
   * @param typeDamageRelation 属性克制关系实体对象，不能为空
   * @return 影响的行数，成功插入返回1
   */
  int insert(TypeDamageRelation typeDamageRelation);

  /**
   * 根据复合主键更新属性克制关系
   *
   * <p>根据攻击方ID和防御方ID更新对应的伤害倍数 只更新 multiplier 字段，其他字段作为查询条件
   *
   * @param typeDamageRelation 属性克制关系实体对象，包含更新后的数据
   * @return 影响的行数，成功更新返回1，记录不存在返回0
   */
  int update(TypeDamageRelation typeDamageRelation);

  /**
   * 根据复合主键删除属性克制关系
   *
   * <p>根据攻击方ID和防御方ID删除对应的克制关系记录 删除操作不可逆，请谨慎使用
   *
   * @param attackingTypeId 攻击者属性ID
   * @param defendingTypeId 攻击者属性ID
   * @return 影响的行数，成功删除返回1，记录不存在返回0
   */
  int deleteByAttackingTypeIdAndDefendingTypeId(Long attackingTypeId, Long defendingTypeId);

  /**
   * 根据复合主键查询属性克制关系
   *
   * <p>根据攻击方属性ID和防御方属性ID查询对应的克制关系记录 用于获取特定属性组合的伤害倍数信息
   *
   * @param attackingTypeId 攻击方属性ID，不能为空
   * @param defendingTypeId 防御方属性ID，不能为空
   * @return 包含查询结果的 Optional 对象，如果记录存在则返回实体对象，否则返回空 Optional
   */
  Optional<TypeDamageRelation> selectByAttackingTypeIdAndDefendingTypeId(
      Long attackingTypeId, Long defendingTypeId);

  /**
   * 分页查询属性克制关系列表（支持动态条件）
   *
   * <p>根据提供的查询条件和分页参数进行分页查询 支持按攻击方ID、防御方ID、伤害倍数等条件进行筛选 按攻击方ID和防御方ID升序排列返回分页结果
   *
   * @param page 分页参数，包含页码、每页大小等信息
   * @param typeDamageRelation 查询条件对象，包含筛选条件
   * @return 分页结果，包含符合条件的属性克制关系列表和分页信息
   */
  IPage<TypeDamageRelation> selectList(
      Page<TypeDamageRelation> page, TypeDamageRelation typeDamageRelation);

  /**
   * 查询属性克制关系列表（支持动态条件）
   *
   * <p>根据提供的查询条件查询属性克制关系列表 支持按攻击方ID、防御方ID、伤害倍数等条件进行筛选 按攻击方ID和防御方ID升序排列返回结果列表
   *
   * @param typeDamageRelation 查询条件对象，包含筛选条件
   * @return 符合条件的属性克制关系列表
   */
  List<TypeDamageRelation> selectList(TypeDamageRelation typeDamageRelation);
}
