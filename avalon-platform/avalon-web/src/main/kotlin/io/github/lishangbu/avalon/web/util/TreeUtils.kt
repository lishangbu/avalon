package io.github.lishangbu.avalon.web.util

import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * 树结构处理工具
 *
 * 提供树的构建、查找、路径获取、扁平化、过滤和遍历等常用操作
 *
 * @author lishangbu
 * @since 2025/08/25
 */
object TreeUtils {
    /** 将列表构建为树结构 */
    @JvmStatic
    fun <T : Any, I> buildTree(
        list: List<T>?,
        idGetter: Function<T, I>,
        parentIdGetter: Function<T, I>,
        childrenSetter: BiConsumer<T, List<T>>,
    ): List<T> {
        if (list.isNullOrEmpty()) {
            return emptyList()
        }

        val nodeMap = list.associateBy { idGetter.apply(it) }
        val roots = mutableListOf<T>()

        for (node in list) {
            val parentId = parentIdGetter.apply(node)
            if (parentId == null) {
                roots += node
                continue
            }

            val parentNode = nodeMap[parentId]
            if (parentNode != null) {
                var children = getChildren(parentNode)
                if (children == null) {
                    children = mutableListOf()
                    childrenSetter.accept(parentNode, children)
                }
                children.add(node)
            } else {
                roots += node
            }
        }

        return roots
    }

    /** 获取子节点列表 */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getChildren(node: T): MutableList<T>? =
        try {
            node::class.java.getMethod("getChildren").invoke(node) as? MutableList<T>
        } catch (_: Exception) {
            null
        }

    /** 查找首个匹配节点 */
    @JvmStatic
    fun <T : Any> findNode(
        tree: List<T>?,
        predicate: Predicate<T>,
        childrenGetter: Function<T, List<T>?>,
    ): T? {
        if (tree.isNullOrEmpty()) {
            return null
        }

        for (node in tree) {
            if (predicate.test(node)) {
                return node
            }
            val children = childrenGetter.apply(node)
            if (!children.isNullOrEmpty()) {
                val found = findNode(children, predicate, childrenGetter)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

    /** 查找所有匹配节点 */
    @JvmStatic
    fun <T : Any> findNodes(
        tree: List<T>?,
        predicate: Predicate<T>,
        childrenGetter: Function<T, List<T>?>,
    ): List<T> {
        val result = mutableListOf<T>()
        findNodesInternal(tree, predicate, childrenGetter, result)
        return result
    }

    /** 递归收集匹配节点 */
    private fun <T : Any> findNodesInternal(
        nodes: List<T>?,
        predicate: Predicate<T>,
        childrenGetter: Function<T, List<T>?>,
        result: MutableList<T>,
    ) {
        if (nodes.isNullOrEmpty()) {
            return
        }

        for (node in nodes) {
            if (predicate.test(node)) {
                result += node
            }
            val children = childrenGetter.apply(node)
            if (!children.isNullOrEmpty()) {
                findNodesInternal(children, predicate, childrenGetter, result)
            }
        }
    }

    /** 获取目标节点路径 */
    @JvmStatic
    fun <T : Any> getNodePath(
        tree: List<T>?,
        targetPredicate: Predicate<T>,
        childrenGetter: Function<T, List<T>?>,
    ): List<T> {
        val path = mutableListOf<T>()
        findPath(tree, targetPredicate, childrenGetter, path)
        return path
    }

    /** 递归查找节点路径 */
    private fun <T : Any> findPath(
        nodes: List<T>?,
        targetPredicate: Predicate<T>,
        childrenGetter: Function<T, List<T>?>,
        path: MutableList<T>,
    ): Boolean {
        if (nodes.isNullOrEmpty()) {
            return false
        }

        for (node in nodes) {
            path += node
            if (targetPredicate.test(node)) {
                return true
            }
            val children = childrenGetter.apply(node)
            if (
                !children.isNullOrEmpty() &&
                findPath(children, targetPredicate, childrenGetter, path)
            ) {
                return true
            }
            path.removeAt(path.lastIndex)
        }
        return false
    }

    /** 将树结构扁平化为列表 */
    @JvmStatic
    fun <T : Any> flattenTree(
        tree: List<T>?,
        childrenGetter: Function<T, List<T>?>,
    ): List<T> {
        val result = mutableListOf<T>()
        flattenTreeInternal(tree, childrenGetter, result)
        return result
    }

    /** 按条件过滤树结构 */
    @JvmStatic
    fun <T : Any> filterTree(
        tree: List<T>?,
        predicate: Predicate<T>,
        childrenGetter: Function<T, List<T>?>,
        childrenSetter: BiConsumer<T, List<T>?>,
    ): List<T> {
        if (tree.isNullOrEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<T>()
        for (node in tree) {
            val copyNode = tryCreateCopy(node)
            val children = childrenGetter.apply(node)
            if (!children.isNullOrEmpty()) {
                val filteredChildren =
                    filterTree(children, predicate, childrenGetter, childrenSetter)
                childrenSetter.accept(copyNode, filteredChildren)
            } else {
                childrenSetter.accept(
                    copyNode,
                    if (children == null) null else emptyList(),
                )
            }

            val copyNodeChildren = childrenGetter.apply(copyNode)
            if (predicate.test(node) || !copyNodeChildren.isNullOrEmpty()) {
                result += copyNode
            }
        }
        return result
    }

    /** 尝试创建节点副本 */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> tryCreateCopy(node: T): T =
        try {
            val copyNode = node::class.java.getDeclaredConstructor().newInstance() as T
            for (field in getAllFields(node::class.java)) {
                if (Modifier.isStatic(field.modifiers)) {
                    continue
                }
                field.isAccessible = true
                if (field.name != "children") {
                    field.set(copyNode, field.get(node))
                }
            }
            copyNode
        } catch (ex: Exception) {
            log.error("Failed to create or copy node instance: {}", ex.message)
            node
        }

    /** 遍历树结构 */
    @JvmStatic
    fun <T : Any> traverseTree(
        tree: List<T>?,
        action: BiConsumer<T, Int>,
        childrenGetter: Function<T, List<T>?>,
    ) {
        traverseTreeInternal(tree, action, childrenGetter, 0)
    }

    /** 获取树的最大深度 */
    @JvmStatic
    fun <T : Any> getMaxDepth(
        tree: List<T>?,
        childrenGetter: Function<T, List<T>?>,
    ): Int {
        if (tree.isNullOrEmpty()) {
            return 0
        }

        var maxDepth = 0
        for (node in tree) {
            val children = childrenGetter.apply(node)
            if (!children.isNullOrEmpty()) {
                maxDepth = maxOf(maxDepth, getMaxDepth(children, childrenGetter))
            }
        }
        return maxDepth + 1
    }

    /** 根据 ID 查找节点 */
    @JvmStatic
    fun <T : Any, I> findNodeById(
        tree: List<T>?,
        id: I,
        idGetter: Function<T, I>,
        childrenGetter: Function<T, List<T>?>,
    ): T? =
        findNode(
            tree,
            Predicate { node -> idGetter.apply(node) == id },
            childrenGetter,
        )

    /** 获取类型的全部字段 */
    private fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        fields += clazz.declaredFields
        val superClass = clazz.superclass
        if (superClass != null && superClass != Any::class.java) {
            fields += getAllFields(superClass)
        }
        return fields
    }

    /** 递归遍历树结构 */
    private fun <T : Any> traverseTreeInternal(
        nodes: List<T>?,
        action: BiConsumer<T, Int>,
        childrenGetter: Function<T, List<T>?>,
        level: Int,
    ) {
        if (nodes.isNullOrEmpty()) {
            return
        }
        for (node in nodes) {
            action.accept(node, level)
            val children = childrenGetter.apply(node)
            if (!children.isNullOrEmpty()) {
                traverseTreeInternal(children, action, childrenGetter, level + 1)
            }
        }
    }

    /** 递归扁平化树结构 */
    private fun <T : Any> flattenTreeInternal(
        nodes: List<T>?,
        childrenGetter: Function<T, List<T>?>,
        result: MutableList<T>,
    ) {
        if (nodes.isNullOrEmpty()) {
            return
        }
        for (node in nodes) {
            result += node
            val children = childrenGetter.apply(node)
            if (!children.isNullOrEmpty()) {
                flattenTreeInternal(children, childrenGetter, result)
            }
        }
    }

    /** 日志记录器 */
    private val log = LoggerFactory.getLogger(TreeUtils::class.java)
}
