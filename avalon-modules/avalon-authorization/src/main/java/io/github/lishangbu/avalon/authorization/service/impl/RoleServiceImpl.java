package io.github.lishangbu.avalon.authorization.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.mapper.RoleMapper;
import io.github.lishangbu.avalon.authorization.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 角色信息服务实现类
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
  private final RoleMapper roleMapper;

  /**
   * 分页查询角色信息
   *
   * @param pageNum 当前分页
   * @param pageSize 分页大小
   * @param role 查询条件
   * @return 包含分页的角色信息
   */
  @Override
  public PageInfo<Role> getPage(Integer pageNum, Integer pageSize, Role role) {
    return PageHelper.startPage(pageNum, pageSize)
        .doSelectPageInfo(() -> roleMapper.selectAll(role));
  }
}
