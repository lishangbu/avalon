package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.model.MenuTreeNode
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.service.MenuService
import io.github.lishangbu.avalon.web.util.TreeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import java.util.*

/**
 * 菜单服务接口实现
 *
 * 提供根据角色代码构建菜单树的实现
 *
 * @author lishangbu
 * @since 2025/9/19
 */
@Service
class MenuServiceImpl(
    private val menuRepository: MenuRepository,
) : MenuService {
    override fun listMenuTreeByRoleCodes(roleCodes: List<String>): List<MenuTreeNode> {
        if (CollectionUtils.isEmpty(roleCodes)) {
            return emptyList()
        }
        val menus = menuRepository.findAllByRoleCodes(roleCodes)
        log.debug("根据角色编码获取到 [{}] 条菜单记录", menus.size)
        return buildTreeFromMenus(menus)
    }

    override fun listAllMenuTree(menu: Menu): List<MenuTreeNode> {
        val condition = normalizeCondition(menu)
        val allMenus = menuRepository.findAllByOrderBySortingOrderAscIdAsc()
        if (CollectionUtils.isEmpty(allMenus)) {
            return emptyList()
        }
        if (!hasQueryCondition(condition)) {
            return buildTreeFromMenus(allMenus)
        }

        val matchedMenus =
            menuRepository.findAll(Example.of(condition, MENU_QUERY_MATCHER), MENU_TREE_SORT)
        if (CollectionUtils.isEmpty(matchedMenus)) {
            return emptyList()
        }

        val menuById = allMenus.associateBy { it.id }
        val childrenByParentId = allMenus.groupBy { it.parentId }

        val includedIds = linkedSetOf<Long>()
        for (matchedMenu in matchedMenus) {
            collectAncestors(menuById, matchedMenu.id, includedIds)
            collectDescendants(matchedMenu.id, childrenByParentId, includedIds)
        }

        if (includedIds.isEmpty()) {
            return emptyList()
        }

        val filteredMenus =
            allMenus.filter { menuItem -> includedIds.contains(menuItem.id) }

        return buildTreeFromMenus(filteredMenus)
    }

    override fun getById(id: Long): java.util.Optional<Menu> = menuRepository.findById(id)

    @Transactional(rollbackFor = [Exception::class])
    override fun save(menu: Menu): Menu = menuRepository.save(menu)

    @Transactional(rollbackFor = [Exception::class])
    override fun update(menu: Menu): Menu = menuRepository.save(menu)

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        menuRepository.deleteById(id)
    }

    /**
     * 将权限实体列表转换为树节点并构建树结构
     * - 处理空集合，返回不可变空列表
     * - 将 Menu 映射为 MenuTreeNode
     * - 使用通用的 [TreeUtils] 构建树
     *
     * @param menus 权限实体列表，允许为 null
     * @return 树结构的 MenuTreeNode 列表，永远不返回 null
     * @see TreeUtils#buildTree(List, Function, Function, BiConsumer)
     */
    private fun buildTreeFromMenus(menus: List<Menu>?): List<MenuTreeNode> {
        if (CollectionUtils.isEmpty(menus)) {
            return emptyList()
        }

        val treeNodes = menus!!.map(::MenuTreeNode)

        return TreeUtils.buildTree(
            treeNodes,
            { it.id },
            { it.parentId },
            { parent, children -> parent.children = children },
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MenuServiceImpl::class.java)

        private val MENU_QUERY_MATCHER: ExampleMatcher =
            ExampleMatcher
                .matching()
                .withIgnoreNullValues()
                .withMatcher("icon", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("key", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("label", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("path", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("redirect", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("component", ExampleMatcher.GenericPropertyMatchers.contains())

        private val MENU_TREE_SORT: Sort =
            Sort.by(Sort.Order.asc("sortingOrder"), Sort.Order.asc("id"))

        private fun collectAncestors(
            menuById: Map<Long, Menu>,
            startId: Long,
            includedIds: MutableSet<Long>,
        ) {
            var currentId: Long? = startId
            while (currentId != null && includedIds.add(currentId)) {
                val current = menuById[currentId] ?: break
                currentId = current.parentId
            }
        }

        private fun collectDescendants(
            startId: Long?,
            childrenByParentId: Map<Long?, List<Menu>>,
            includedIds: MutableSet<Long>,
        ) {
            if (startId == null) {
                return
            }
            val deque: ArrayDeque<Long> = ArrayDeque()
            val visited: MutableSet<Long> = LinkedHashSet()
            deque.push(startId)
            while (!deque.isEmpty()) {
                val currentId = deque.pop()
                if (!visited.add(currentId)) {
                    continue
                }
                includedIds.add(currentId)
                for (child in childrenByParentId[currentId] ?: Collections.emptyList()) {
                    deque.push(child.id)
                }
            }
        }

        private fun normalizeCondition(menu: Menu?): Menu {
            if (menu == null) {
                return Menu {}
            }
            return Menu {
                menu.readOrNull { id }?.let { id = it }
                parentId = menu.readOrNull { parentId }
                disabled = menu.readOrNull { disabled }
                extra = trimToNull(menu.readOrNull { extra })
                icon = trimToNull(menu.readOrNull { icon })
                key = trimToNull(menu.readOrNull { key })
                label = trimToNull(menu.readOrNull { label })
                show = menu.readOrNull { show }
                path = trimToNull(menu.readOrNull { path })
                name = trimToNull(menu.readOrNull { name })
                redirect = trimToNull(menu.readOrNull { redirect })
                component = trimToNull(menu.readOrNull { component })
                sortingOrder = menu.readOrNull { sortingOrder }
                pinned = menu.readOrNull { pinned }
                showTab = menu.readOrNull { showTab }
                enableMultiTab = menu.readOrNull { enableMultiTab }
            }
        }

        private fun trimToNull(value: String?): String? = if (StringUtils.hasText(value)) value!!.trim { it <= ' ' } else null

        private fun hasQueryCondition(menu: Menu?): Boolean =
            menu != null &&
                (
                    menu.readOrNull { id } != null ||
                        menu.readOrNull { parentId } != null ||
                        menu.readOrNull { disabled } != null ||
                        menu.readOrNull { extra } != null ||
                        menu.readOrNull { icon } != null ||
                        menu.readOrNull { key } != null ||
                        menu.readOrNull { label } != null ||
                        menu.readOrNull { show } != null ||
                        menu.readOrNull { path } != null ||
                        menu.readOrNull { name } != null ||
                        menu.readOrNull { redirect } != null ||
                        menu.readOrNull { component } != null ||
                        menu.readOrNull { sortingOrder } != null ||
                        menu.readOrNull { pinned } != null ||
                        menu.readOrNull { showTab } != null ||
                        menu.readOrNull { enableMultiTab } != null
                )

        private inline fun <T, R> T?.readOrNull(block: T.() -> R): R? = this?.let { runCatching { it.block() }.getOrNull() }
    }
}
