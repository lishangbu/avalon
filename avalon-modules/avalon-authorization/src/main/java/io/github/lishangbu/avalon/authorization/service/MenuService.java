package io.github.lishangbu.avalon.authorization.service;

import io.github.lishangbu.avalon.authorization.entity.Menu;
import io.github.lishangbu.avalon.authorization.model.MenuTreeNode;
import java.util.List;
import java.util.Optional;

/// 菜单服务接口
///
/// 提供根据角色获取菜单树的能力
///
/// @author lishangbu
/// @since 2025/8/28
public interface MenuService {
    /// 根据角色代码获取菜单树
    ///
    /// @param roleCodes 角色代码
    /// @return 菜单树节点列表
    List<MenuTreeNode> listMenuTreeByRoleCodes(List<String> roleCodes);

    /// 查询全量菜单树（支持按完整菜单条件筛选）
    ///
    /// @param menu 菜单查询条件，可为空
    /// @return 菜单树节点列表
    List<MenuTreeNode> listAllMenuTree(Menu menu);

    /// 根据 ID 查询菜单。
    Optional<Menu> getById(Long id);

    /// 新增菜单。
    Menu save(Menu menu);

    /// 更新菜单。
    Menu update(Menu menu);

    /// 根据 ID 删除菜单。
    void removeById(Long id);
}
