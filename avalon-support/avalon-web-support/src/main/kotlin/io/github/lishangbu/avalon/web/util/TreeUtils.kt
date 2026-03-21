package io.github.lishangbu.avalon.web.util

import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * 树形结构工具类，用于处理树形数据 提供将列表转换为树、查找节点、扁平化、过滤和遍历等常用操作
 *
 * @param list 待转换的列表
 * @param idGetter 获取节点ID的函数
 * @param parentIdGetter 获取父节点ID的函数
 * @param childrenSetter 设置子节点的函数
 * @param <T> 节点类型
 * @param <I> ID类型
 * @param tree 树结构
 * @param predicate 匹配条件
 * @param childrenGetter 获取子节点的函数
 * @param tree 树结构
 * @param targetPredicate 目标节点匹配条件
 * @param childrenGetter 获取子节点的函数
 * @param <T> 节点类型
 * @param predicate 过滤条件
 * @param action 要执行的操作 计算树的最大深度
 * @param id 目标节点ID
 * @return 树形结构的根节点列表 构建节点映射表 (ID -> 节点) 遍历所有节点，将它们添加到父节点的子节点列表中 如果父ID为null，则为根节点 尝试获取父节点
 *   获取父节点的子节点列表，如果不存在则创建新列表 找不到父节点，作为根节点处理 获取节点的子节点列表。注意：这是一个辅助方法，尝试通过反射调用 `getChildren()`
 *   这里需要根据实际情况实现，一个常见方式是节点类有getChildren方法 这里使用反射模拟获取children字段 在树中查找符合条件的第一个节点
 * @return 找到的节点，没有则返回 null 检查当前节点 递归检查子节点 在树中查找所有符合条件的节点
 * @return 找到的节点列表 查找节点的内部递归方法 获取从根节点到目标节点的路径
 * @return 路径节点列表，如果找不到目标节点则返回空列表 查找路径的内部递归方法 添加当前节点到路径 检查是否找到目标节点 当前路径不包含目标节点，移除此节点 树形结构扁平化为列表
 * @return 扁平化后的列表 过滤树节点，保持树形结构
 * @return 过滤后的树结构 深拷贝节点以避免修改原树 首先创建新实例 复制所有非静态字段的值 跳过静态字段 不复制children字段，这个字段会由childrenSetter单独处理
 *   处理子节点 确保copyNode的子节点列表为null或空列表 如果当前节点满足条件，或者它有满足条件的子节点，则添加到结果中 遍历树结构，对每个节点执行操作
 * @return 树的最大深度 根据节点ID查找节点
 * @return 找到的节点，没有则返回null 获取类的所有字段，包括继承的字段 获取当前类的字段 递归获取父类的字段 遍历树的内部递归方法 对当前节点执行操作 递归处理子节点
 *   扁平化树的内部递归方法 // 构建节点映射表 (ID -> 节点) // 遍历所有节点，将它们添加到父节点的子节点列表中 // 如果父ID为null，则为根节点 // 尝试获取父节点 //
 *   获取父节点的子节点列表，如果不存在则创建新列表 // 找不到父节点，作为根节点处理 // 这里需要根据实际情况实现，一个常见方式是节点类有getChildren方法 //
 *   这里使用反射模拟获取children字段 // 检查当前节点 // 递归检查子节点 // 添加当前节点到路径 // 检查是否找到目标节点 // 当前路径不包含目标节点，移除此节点 //
 *   深拷贝节点以避免修改原树 // 首先创建新实例 // 复制所有非静态字段的值 // 跳过静态字段 // 不复制children字段，这个字段会由childrenSetter单独处理 //
 *   处理子节点 // 确保copyNode的子节点列表为null或空列表 // 如果当前节点满足条件，或者它有满足条件的子节点，则添加到结果中 // 获取当前类的字段 // 递归获取父类的字段
 *   // 对当前节点执行操作 // 递归处理子节点
 * @author lishangbu
 * @since 2025/08/25 将列表转换为树形结构
 */

/** 树形结构工具类。 */
object TreeUtils {
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
                    children = ArrayList()
                    childrenSetter.accept(parentNode, children)
                }
                children.add(node)
            } else {
                roots += node
            }
        }

        return roots
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getChildren(node: T): MutableList<T>? =
        try {
            node::class.java.getMethod("getChildren").invoke(node) as? MutableList<T>
        } catch (_: Exception) {
            null
        }

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

    @JvmStatic
    fun <T : Any> flattenTree(
        tree: List<T>?,
        childrenGetter: Function<T, List<T>?>,
    ): List<T> {
        val result = mutableListOf<T>()
        flattenTreeInternal(tree, childrenGetter, result)
        return result
    }

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
                    if (children == null) null else Collections.emptyList(),
                )
            }

            val copyNodeChildren = childrenGetter.apply(copyNode)
            if (predicate.test(node) || !copyNodeChildren.isNullOrEmpty()) {
                result += copyNode
            }
        }
        return result
    }

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

    @JvmStatic
    fun <T : Any> traverseTree(
        tree: List<T>?,
        action: BiConsumer<T, Int>,
        childrenGetter: Function<T, List<T>?>,
    ) {
        traverseTreeInternal(tree, action, childrenGetter, 0)
    }

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

    @JvmStatic
    fun <T : Any, I> findNodeById(
        tree: List<T>?,
        id: I,
        idGetter: Function<T, I>,
        childrenGetter: Function<T, List<T>?>,
    ): T? =
        findNode(
            tree,
            Predicate { node -> Objects.equals(idGetter.apply(node), id) },
            childrenGetter,
        )

    private fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        fields += clazz.declaredFields
        val superClass = clazz.superclass
        if (superClass != null && superClass != Any::class.java) {
            fields += getAllFields(superClass)
        }
        return fields
    }

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

    private val log = LoggerFactory.getLogger(TreeUtils::class.java)
}
