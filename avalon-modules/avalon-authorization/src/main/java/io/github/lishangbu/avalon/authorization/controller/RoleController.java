package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.service.RoleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 角色控制器
///
/// 提供角色相关的 REST 接口（占位）
///
/// @author lishangbu
/// @since 2025/8/30
@RequestMapping("/role")
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /// 分页条件查询角色
    ///
    /// @param pageable 分页参数
    /// @param role     查询条件
    /// @return 角色分页结果
    @GetMapping("/page")
    public Page<Role> getRolePage(Pageable pageable, Role role) {
        return roleService.getPageByCondition(role, pageable);
    }

    /// 条件查询角色列表
    ///
    /// @param role 查询条件
    /// @return 角色列表
    @GetMapping("/list")
    public List<Role> listRoles(Role role) {
        return roleService.listByCondition(role);
    }

    /// 根据 ID 查询角色
    ///
    /// @param id 角色 ID
    /// @return 角色信息
    @GetMapping("/{id:\\d+}")
    public Role getById(@PathVariable Long id) {
        return roleService.getById(id).orElse(null);
    }

    /// 新增角色
    ///
    /// @param role 角色实体
    /// @return 保存后的角色
    @PostMapping
    public Role save(@RequestBody Role role) {
        return roleService.save(role);
    }

    /// 更新角色
    ///
    /// @param role 角色实体
    /// @return 更新后的角色
    @PutMapping
    public Role update(@RequestBody Role role) {
        return roleService.update(role);
    }

    /// 根据 ID 删除角色
    ///
    /// @param id 角色 ID
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        roleService.removeById(id);
    }
}
