package io.github.lishangbu.avalon.authorization.service;

import com.github.pagehelper.PageInfo;
import io.github.lishangbu.avalon.authorization.entity.Role;

/**
 * 角色服务
 *
 * @author lishangbu
 * @since 2025/8/30
 */
public interface RoleService {

  /**
   * 分页查询角色信息
   *
   * @param pageNum 当前分页
   * @param pageSize 分页大小
   * @param role 查询条件
   * @return 包含分页的角色信息
   */
  PageInfo<Role> getPage(Integer pageNum, Integer pageSize, Role role);
}
