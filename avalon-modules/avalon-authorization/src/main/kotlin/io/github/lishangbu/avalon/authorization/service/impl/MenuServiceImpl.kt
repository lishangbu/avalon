package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.MenuTreeView
import io.github.lishangbu.avalon.authorization.entity.dto.MenuView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveMenuInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateMenuInput
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.authorization.service.MenuService
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 菜单服务实现
 *
 * 负责菜单查询与菜单树构建
 *
 * @author lishangbu
 * @since 2025/9/19
 */
@Service
class MenuServiceImpl(
    /** 菜单仓储 */
    private val menuRepository: MenuRepository,
    /** 角色仓储 */
    private val roleRepository: RoleRepository,
) : MenuService {
    /** 根据角色编码列表查询菜单树列表 */
    override fun listMenuTreeByRoleCodes(roleCodes: List<String>): List<MenuTreeView> {
        if (roleCodes.isEmpty()) {
            return emptyList()
        }
        val menus = menuRepository.listViewsByRoleCodes(roleCodes)
        log.debug("根据角色编码获取到 [{}] 条菜单记录", menus.size)
        return buildRoleMenuTree(menus)
    }

    /** 查询全部菜单树列表 */
    override fun listTree(): List<MenuTreeView> = menuRepository.listTreeViews()

    /** 按条件查询菜单列表 */
    override fun listByCondition(specification: MenuSpecification): List<MenuView> = menuRepository.listViews(specification)

    /** 按 ID 查询菜单 */
    override fun getById(id: Long): MenuView? = menuRepository.loadViewById(id)

    /** 保存菜单 */
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveMenuInput): MenuView {
        val menu = command.toEntity()
        validateParent(menu)
        return menuRepository.save(menu, SaveMode.INSERT_ONLY).let(::reloadView)
    }

    /** 更新菜单 */
    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateMenuInput): MenuView {
        val menu = command.toEntity()
        validateParent(menu)
        return menuRepository.save(menu).let(::reloadView)
    }

    /** 按 ID 删除菜单 */
    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        if (menuRepository.hasChildren(id)) {
            throw IllegalStateException("请先删除子菜单后再删除当前菜单")
        }
        detachMenuFromRoles(id)
        menuRepository.deleteById(id)
    }

    private fun validateParent(menu: Menu) {
        val menuId = menu.readOrNull { id }
        val parentId = menu.readOrNull { parent }?.id ?: return

        if (menuId != null && parentId == menuId) {
            throw IllegalArgumentException("父菜单不能选择自身")
        }

        val parent =
            menuRepository.findNullable(parentId, AuthorizationFetchers.MENU)
                ?: throw IllegalArgumentException("父菜单不存在")

        if (menuId != null) {
            validateNoCircularReference(menuId, parent)
        }
    }

    private fun validateNoCircularReference(
        menuId: Long,
        parent: Menu,
    ) {
        var currentId: Long? = parent.id
        val visited = mutableSetOf<Long>()

        while (currentId != null) {
            if (currentId == menuId) {
                throw IllegalStateException("父菜单不能选择当前菜单或其子菜单")
            }
            if (!visited.add(currentId)) {
                throw IllegalStateException("菜单层级存在循环引用")
            }
            currentId =
                menuRepository
                    .findNullable(currentId, AuthorizationFetchers.MENU)
                    ?.readOrNull { parent }
                    ?.id
        }
    }

    private fun detachMenuFromRoles(menuId: Long) {
        val roles =
            roleRepository
                .listWithMenus(null)
                .filter { role -> role.menus.any { menu -> menu.id == menuId } }

        roles.forEach { role ->
            val remainedMenus = role.menus.filterNot { menu -> menu.id == menuId }
            roleRepository.save(
                Role(role) {
                    menus = remainedMenus
                },
            )
        }
    }

    private fun buildRoleMenuTree(menus: List<MenuView>): List<MenuTreeView> {
        if (menus.isEmpty()) {
            return emptyList()
        }
        val ids = menus.mapTo(linkedSetOf()) { it.id }
        val childrenByParentId = menus.groupBy { it.parentId }

        fun toTree(
            node: MenuView,
            path: Set<String> = emptySet(),
        ): MenuTreeView {
            if (node.id in path) {
                throw IllegalStateException("菜单层级存在循环引用")
            }
            val children =
                childrenByParentId[node.id]
                    .orEmpty()
                    .map { child -> toTree(child, path + node.id) }
            return MenuTreeView(
                id = node.id,
                parentId = node.parentId,
                disabled = node.disabled,
                extra = node.extra,
                icon = node.icon,
                key = node.key,
                label = node.label,
                show = node.show,
                path = node.path,
                name = node.name,
                redirect = node.redirect,
                component = node.component,
                sortingOrder = node.sortingOrder,
                pinned = node.pinned,
                showTab = node.showTab,
                enableMultiTab = node.enableMultiTab,
                children = children,
            )
        }

        return menus
            .filter { menu -> menu.parentId == null || menu.parentId !in ids }
            .map(::toTree)
    }

    companion object {
        /** 日志记录器 */
        private val log: Logger = LoggerFactory.getLogger(MenuServiceImpl::class.java)
    }

    private fun reloadView(menu: Menu): MenuView = requireNotNull(menuRepository.loadViewById(menu.id)) { "未找到 ID=${menu.id} 对应的菜单" }
}
