package io.github.lishangbu.avalon.authorization.controller;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.PageParam;
import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色控制器
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@RequestMapping("/role")
@RestController
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;

  /**
   * 分页查询角色
   *
   * @param pageParam 分页参数
   * @param role 角色查询条件
   * @return 角色分页结果
   */
  @GetMapping("/page")
  public PageInfo<Role> getPage(PageParam pageParam, Role role) {
    return roleService.getPage(pageParam.getPageNum(), pageParam.getPageSize(), role);
  }
}
