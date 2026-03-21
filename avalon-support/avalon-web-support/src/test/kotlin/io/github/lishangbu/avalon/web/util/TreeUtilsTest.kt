package io.github.lishangbu.avalon.web.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

/**
 * 树形结构工具类单元测试 覆盖 TreeUtils 的构建、查找、遍历、扁平化及过滤等功能
 *
 * @author lishangbu
 * @since 2025/08/25 准备扁平节点列表 准备树形结构 使用TreeUtils构建树 验证结果 查找名称为"子节点1-1"的节点 查找不存在的节点 查找所有包含"子节点"的节点
 *   获取到"子节点1-1-1"的路径 获取不存在节点的路径 扁平化树结构 验证是否包含所有节点 扁平化空树 过滤树，只保留名称中包含"1"的节点及其父节点
 *   根节点1应该被保留，因为它的名字包含"1" 检查子节点结构是否正确 准备一个没有子节点的树 过滤树 用来计数的数组 遍历树，对每个节点执行操作 获取树的最大深度 获取空树的最大深度
 *   根据ID查找节点 根据不存在的ID查找节点 测试用的树节点类 // 准备扁平节点列表 // 准备树形结构 // 使用TreeUtils构建树 // 验证结果 //
 *   查找名称为"子节点1-1"的节点 // 查找不存在的节点 // 查找所有包含"子节点"的节点 // 获取到"子节点1-1-1"的路径 // 获取不存在节点的路径 // 扁平化树结构 //
 *   验证是否包含所有节点 // 扁平化空树 // 过滤树，只保留名称中包含"1"的节点及其父节点 // 根节点1应该被保留，因为它的名字包含"1" // 检查子节点结构是否正确 //
 *   准备一个没有子节点的树 // 过滤树 // 用来计数的数组 // 遍历树，对每个节点执行操作 // 获取树的最大深度 // 获取空树的最大深度 // 根据ID查找节点 //
 *   根据不存在的ID查找节点
 */
class TreeUtilsTest {
    private lateinit var flatNodes: List<TreeNode>
    private lateinit var treeNodes: List<TreeNode>

    @BeforeEach
    fun setUp() {
        flatNodes =
            listOf(
                TreeNode(1L, null, "根节点1", null),
                TreeNode(2L, null, "根节点2", null),
                TreeNode(3L, 1L, "子节点1-1", null),
                TreeNode(4L, 1L, "子节点1-2", null),
                TreeNode(5L, 2L, "子节点2-1", null),
                TreeNode(6L, 3L, "子节点1-1-1", null),
                TreeNode(7L, 3L, "子节点1-1-2", null),
            )

        val node6 = TreeNode(6L, 3L, "子节点1-1-1", mutableListOf())
        val node7 = TreeNode(7L, 3L, "子节点1-1-2", mutableListOf())
        val node3 = TreeNode(3L, 1L, "子节点1-1", listOf(node6, node7))
        val node4 = TreeNode(4L, 1L, "子节点1-2", mutableListOf())
        val node5 = TreeNode(5L, 2L, "子节点2-1", mutableListOf())
        val node1 = TreeNode(1L, null, "根节点1", listOf(node3, node4))
        val node2 = TreeNode(2L, null, "根节点2", listOf(node5))

        treeNodes = listOf(node1, node2)
    }

    @Test
    fun buildTree_shouldCreateCorrectTreeStructure() {
        val result =
            TreeUtils.buildTree(
                flatNodes,
                { it.id },
                { it.parentId },
                { node, children -> node.children = children },
            )

        assertEquals(2, result.size)

        val root1 = result[0]
        assertEquals(1L, root1.id)
        assertEquals(2, root1.children!!.size)

        val child1 = root1.children!![0]
        assertEquals(3L, child1.id)
        assertEquals(2, child1.children!!.size)

        val root2 = result[1]
        assertEquals(2L, root2.id)
        assertEquals(1, root2.children!!.size)
    }

    @Test
    fun findNode_shouldReturnCorrectNode() {
        val result = TreeUtils.findNode(treeNodes, { it.name == "子节点1-1" }, { it.children })

        assertNotNull(result)
        assertEquals(3L, result!!.id)
        assertEquals("子节点1-1", result.name)
    }

    @Test
    fun findNode_shouldReturnNullWhenNotFound() {
        val result = TreeUtils.findNode(treeNodes, { it.name == "不存在的节点" }, { it.children })

        assertNull(result)
    }

    @Test
    fun findNodes_shouldReturnAllMatchingNodes() {
        val result =
            TreeUtils.findNodes(
                treeNodes,
                { node -> node.name?.contains("子节点") == true },
                { it.children },
            )

        assertEquals(5, result.size)
    }

    @Test
    fun getNodePath_shouldReturnCorrectPath() {
        val result =
            TreeUtils.getNodePath(treeNodes, { Objects.equals(it.id, 6L) }, { it.children })

        assertEquals(3, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(3L, result[1].id)
        assertEquals(6L, result[2].id)
    }

    @Test
    fun getNodePath_shouldReturnEmptyListWhenNotFound() {
        val result =
            TreeUtils.getNodePath(treeNodes, { Objects.equals(it.id, 999L) }, { it.children })

        assertTrue(result.isEmpty())
    }

    @Test
    fun flattenTree_shouldReturnFlatList() {
        val result = TreeUtils.flattenTree(treeNodes) { it.children }

        assertEquals(7, result.size)
        assertTrue(result.any { it.id == 1L })
        assertTrue(result.any { it.id == 3L })
        assertTrue(result.any { it.id == 6L })
    }

    @Test
    fun flattenTree_shouldHandleEmptyTree() {
        val result = TreeUtils.flattenTree(emptyList<TreeNode>()) { it.children }

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterTree_shouldFilterCorrectly() {
        val result =
            TreeUtils.filterTree(
                treeNodes,
                { node -> node.name?.contains("1") == true },
                { it.children },
                { node, children -> node.children = children },
            )

        assertFalse(result.isEmpty())
        assertTrue(result.any { it.id == 1L })
        val root1 = result.firstOrNull { it.id == 1L }
        assertNotNull(root1)
        assertEquals(2, root1!!.children!!.size)
    }

    @Test
    fun filterTree_shouldHandleNullChildren() {
        val nodesWithoutChildren = listOf(TreeNode(1L, null, "根节点1", null))
        val result =
            TreeUtils.filterTree(
                nodesWithoutChildren,
                { true },
                { it.children },
                { node, children -> node.children = children },
            )

        assertEquals(1, result.size)
        assertNull(result[0].children)
    }

    @Test
    fun traverseTree_shouldVisitAllNodes() {
        var count = 0
        val visitedNodeNames = mutableListOf<String?>()

        TreeUtils.traverseTree(
            treeNodes,
            { node, _ ->
                count++
                visitedNodeNames += node.name
            },
            { it.children },
        )

        assertEquals(7, count)
        assertEquals(7, visitedNodeNames.size)
        assertTrue(visitedNodeNames.contains("根节点1"))
        assertTrue(visitedNodeNames.contains("子节点1-1-1"))
    }

    @Test
    fun getMaxDepth_shouldReturnCorrectDepth() {
        val result = TreeUtils.getMaxDepth(treeNodes) { it.children }

        assertEquals(3, result)
    }

    @Test
    fun getMaxDepth_shouldReturnZeroForEmptyTree() {
        val result = TreeUtils.getMaxDepth(emptyList<TreeNode>()) { it.children }

        assertEquals(0, result)
    }

    @Test
    fun findNodeById_shouldReturnCorrectNode() {
        val result = TreeUtils.findNodeById(treeNodes, 6L, { it.id }, { it.children })

        assertNotNull(result)
        assertEquals(6L, result!!.id)
        assertEquals("子节点1-1-1", result.name)
    }

    @Test
    fun findNodeById_shouldReturnNullWhenNotFound() {
        val result = TreeUtils.findNodeById(treeNodes, 999L, { it.id }, { it.children })

        assertNull(result)
    }

    private class TreeNode() {
        var id: Long? = null
        var parentId: Long? = null
        var name: String? = null
        var children: List<TreeNode>? = null

        constructor(id: Long?, parentId: Long?, name: String?, children: List<TreeNode>?) : this() {
            this.id = id
            this.parentId = parentId
            this.name = name
            this.children = children
        }
    }
}
