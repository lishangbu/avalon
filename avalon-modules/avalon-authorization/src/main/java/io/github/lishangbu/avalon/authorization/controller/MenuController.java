package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.authorization.entity.Menu;
import io.github.lishangbu.avalon.authorization.model.MenuTreeNode;
import io.github.lishangbu.avalon.authorization.service.MenuService;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 菜单控制器
///
/// 提供当前用户基于角色的菜单树查询接口
///
/// @author lishangbu
/// @since 2025/8/28
@Slf4j
@RequestMapping("/menu")
@RestController
@RequiredArgsConstructor
public class MenuController {
    private final MenuService menuService;

    /// 获取当前用户角色菜单树
    ///
    /// @param user 当前用户
    /// @return 菜单树
    @GetMapping("/role-tree")
    public List<MenuTreeNode> listCurrentRoleMenuTree(@AuthenticationPrincipal UserInfo user) {
        return menuService.listMenuTreeByRoleCodes(
                user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    /// 查询全量菜单树（支持按完整菜单条件筛选）
    ///
    /// @param menu 菜单查询条件，可为空
    /// @return 菜单树
    @GetMapping("/tree")
    public List<MenuTreeNode> listAllMenuTree(Menu menu) {
        return menuService.listAllMenuTree(menu);
    }

    /// 根据 ID 查询菜单
    ///
    /// @param id 菜单 ID
    /// @return 菜单信息
    @GetMapping("/{id:\\d+}")
    public Menu getById(@PathVariable Long id) {
        return menuService.getById(id).orElse(null);
    }

    /// 新增菜单
    ///
    /// @param menu 菜单实体
    /// @return 保存后的菜单
    @PostMapping
    public Menu save(@RequestBody Menu menu) {
        return menuService.save(menu);
    }

    /// 更新菜单
    ///
    /// @param menu 菜单实体
    /// @return 更新后的菜单
    @PutMapping
    public Menu update(@RequestBody Menu menu) {
        return menuService.update(menu);
    }

    /// 根据 ID 删除菜单
    ///
    /// @param id 菜单 ID
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        menuService.removeById(id);
    }
}
