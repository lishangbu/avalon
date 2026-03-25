package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.Menu
import io.github.lishangbu.avalon.authorization.entity.dto.MenuSpecification
import io.github.lishangbu.avalon.authorization.model.MenuTreeNode
import io.github.lishangbu.avalon.authorization.repository.MenuRepository
import io.github.lishangbu.avalon.authorization.service.MenuService
import io.github.lishangbu.avalon.web.util.TreeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
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
) : MenuService {
    /** 根据角色编码列表查询菜单树列表 */
    override fun listMenuTreeByRoleCodes(roleCodes: List<String>): List<MenuTreeNode> {
        if (roleCodes.isEmpty()) {
            return emptyList()
        }
        val menus = menuRepository.findAllByRoleCodes(roleCodes)
        log.debug("根据角色编码获取到 [{}] 条菜单记录", menus.size)
        return buildTreeFromMenus(menus)
    }

    /** 查询全部菜单树列表 */
    override fun listAllMenuTree(specification: MenuSpecification): List<MenuTreeNode> {
        val allMenus = menuRepository.findAllByOrderBySortingOrderAscIdAsc()
        if (allMenus.isEmpty()) {
            return emptyList()
        }
        val matchedMenus = menuRepository.findAll(specification, MENU_TREE_SORT)
        if (matchedMenus.isEmpty()) {
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

    /** 按 ID 查询菜单 */
    override fun getById(id: Long): Menu? = menuRepository.findById(id)

    /** 保存菜单 */
    @Transactional(rollbackFor = [Exception::class])
    override fun save(menu: Menu): Menu = menuRepository.save(menu)

    /** 更新菜单 */
    @Transactional(rollbackFor = [Exception::class])
    override fun update(menu: Menu): Menu = menuRepository.save(menu)

    /** 按 ID 删除菜单 */
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
     * @param menus 权限实体列表
     * @return 树结构的 MenuTreeNode 列表，永远不返回 null
     * @see TreeUtils#buildTree(List, Function, Function, BiConsumer)
     */
    private fun buildTreeFromMenus(menus: List<Menu>): List<MenuTreeNode> {
        if (menus.isEmpty()) {
            return emptyList()
        }

        val treeNodes = menus.map(::MenuTreeNode)

        return TreeUtils.buildTree(
            treeNodes,
            { it.id },
            { it.parentId },
            { parent, children -> parent.children = children },
        )
    }

    companion object {
        /** 日志记录器 */
        private val log: Logger = LoggerFactory.getLogger(MenuServiceImpl::class.java)

        /** 菜单树排序 */
        private val MENU_TREE_SORT: Sort =
            Sort.by(Sort.Order.asc("sortingOrder"), Sort.Order.asc("id"))

        /** 返回收集祖先节点 */
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

        /** 返回收集后代节点 */
        private fun collectDescendants(
            startId: Long?,
            childrenByParentId: Map<Long?, List<Menu>>,
            includedIds: MutableSet<Long>,
        ) {
            if (startId == null) {
                return
            }
            val deque = ArrayDeque<Long>()
            val visited = mutableSetOf<Long>()
            deque.addLast(startId)
            while (deque.isNotEmpty()) {
                val currentId = deque.removeLast()
                if (!visited.add(currentId)) {
                    continue
                }
                includedIds.add(currentId)
                for (child in childrenByParentId[currentId].orEmpty()) {
                    deque.addLast(child.id)
                }
            }
        }
    }
}
