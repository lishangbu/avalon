package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.UserRoleRelation;
import org.apache.ibatis.annotations.Param;

/**
 * 用户与角色关联的 Mapper，使用 XML 实现 SQL
 *
 * <p>提供对 user_role_relation 中间表的插入与删除操作
 *
 * @author lishangbu
 * @since 2025/12/13
 */
public interface UserRoleRelationMapper {

  /**
   * 插入一条用户与角色的关联记录
   *
   * @param relation 包含 userId 与 roleId 的关联实体
   * @return 受影响的行数
   */
  int insert(UserRoleRelation relation);

  /**
   * 根据用户ID和角色ID删除关联
   *
   * @param userId 用户ID
   * @param roleId 角色ID
   * @return 受影响的行数
   */
  int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

  /**
   * 根据用户ID删除所有角色关联
   *
   * @param userId 用户ID
   * @return 受影响的行数
   */
  int deleteByUserId(@Param("userId") Long userId);

  /**
   * 根据角色ID删除所有用户关联
   *
   * @param roleId 角色ID
   * @return 受影响的行数
   */
  int deleteByRoleId(@Param("roleId") Long roleId);
}
