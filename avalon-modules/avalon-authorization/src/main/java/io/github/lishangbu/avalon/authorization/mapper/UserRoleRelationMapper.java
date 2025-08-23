package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.UserRoleRelation;

/**
 * 用户角色关系(user_role_relation)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface UserRoleRelationMapper {

  /**
   * 新增用户角色关系
   *
   * @param userRoleRelation 实例对象
   * @return 影响行数
   */
  int insert(UserRoleRelation userRoleRelation);

  /**
   * 通过删除用户角色关系
   *
   * @param userId 用户Id
   * @return 影响行数
   */
  int deleteByUserId(Long userId);
}
