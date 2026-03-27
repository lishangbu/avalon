package io.github.lishangbu.avalon.web.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

/**
 * [TreeUtils] 测试
 *
 * 验证树构建、查找、过滤和遍历等核心行为
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
    fun buildTree_shouldReturnEmptyListForNullOrEmptyInput() {
        val nullResult =
            TreeUtils.buildTree<TreeNode, Long?>(
                null,
                { it.id },
                { it.parentId },
                { node, children -> node.children = children },
            )
        val emptyResult =
            TreeUtils.buildTree<TreeNode, Long?>(
                emptyList(),
                { it.id },
                { it.parentId },
                { node, children -> node.children = children },
            )

        assertTrue(nullResult.isEmpty())
        assertTrue(emptyResult.isEmpty())
    }

    @Test
    fun buildTree_shouldTreatOrphanNodeAsRoot() {
        val result =
            TreeUtils.buildTree(
                listOf(
                    TreeNode(1L, null, "根节点", null),
                    TreeNode(2L, 999L, "孤儿节点", null),
                ),
                { it.id },
                { it.parentId },
                { node, children -> node.children = children },
            )

        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun buildTree_shouldHandleNodeWithoutChildrenGetter() {
        val root = NodeWithoutChildrenAccessor(1L, null)
        val child = NodeWithoutChildrenAccessor(2L, 1L)

        val result =
            TreeUtils.buildTree(
                listOf(root, child),
                { it.id },
                { it.parentId },
                { node, children -> node.descendants = children },
            )

        assertEquals(1, result.size)
        assertEquals(1L, result.single().id)
        assertEquals(listOf(2L), result.single().descendants!!.map { it.id })
    }

    @Test
    fun findNode_shouldReturnCorrectNode() {
        val result = TreeUtils.findNode(treeNodes, { it.name == "子节点1-1" }, { it.children })

        assertNotNull(result)
        assertEquals(3L, result!!.id)
        assertEquals("子节点1-1", result.name)
    }

    @Test
    fun findNode_shouldReturnNullForNullTree() {
        val result = TreeUtils.findNode<TreeNode>(null, { it.name == "任意节点" }, { it.children })

        assertNull(result)
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
    fun findNodes_shouldReturnEmptyListForEmptyTree() {
        val result = TreeUtils.findNodes(emptyList<TreeNode>(), { true }, { it.children })

        assertTrue(result.isEmpty())
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
    fun getNodePath_shouldReturnEmptyListForNullTree() {
        val result = TreeUtils.getNodePath<TreeNode>(null, { Objects.equals(it.id, 6L) }, { it.children })

        assertTrue(result.isEmpty())
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
    fun filterTree_shouldReturnEmptyListForEmptyTree() {
        val result =
            TreeUtils.filterTree(
                emptyList<TreeNode>(),
                { true },
                { it.children },
                { node, children -> node.children = children },
            )

        assertTrue(result.isEmpty())
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
    fun filterTree_shouldExcludeNodesWithoutMatchingDescendants() {
        val result =
            TreeUtils.filterTree(
                listOf(TreeNode(1L, null, "根节点", emptyList())),
                { false },
                { it.children },
                { node, children -> node.children = children },
            )

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterTree_shouldCopyInheritedFieldsAndIgnoreStaticFields() {
        InheritedTreeNode.staticCounter = 7
        val node =
            InheritedTreeNode(
                id = 10L,
                parentId = null,
                name = "继承节点",
                inheritedName = "父类字段",
                children = null,
            )

        val result =
            TreeUtils.filterTree(
                listOf(node),
                { true },
                { it.children },
                { current, children -> current.children = children },
            )

        assertEquals(1, result.size)
        assertNotSame(node, result.single())
        assertEquals("父类字段", result.single().inheritedName)
        assertEquals(7, InheritedTreeNode.staticCounter)
    }

    @Test
    fun filterTree_shouldReturnOriginalNodeWhenCopyFails() {
        val node = NoDefaultConstructorNode(1L, "原始节点")

        val result =
            TreeUtils.filterTree(
                listOf(node),
                { true },
                { it.children },
                { current, children -> current.children = children },
            )

        assertSame(node, result.single())
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
    fun traverseTree_shouldIgnoreNullTree() {
        var count = 0

        TreeUtils.traverseTree<TreeNode>(null, { _, _ -> count++ }, { it.children })

        assertEquals(0, count)
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

    private class NodeWithoutChildrenAccessor(
        val id: Long,
        val parentId: Long?,
    ) {
        var descendants: List<NodeWithoutChildrenAccessor>? = null
    }

    private open class BaseInheritedNode {
        var inheritedName: String? = null
    }

    private class InheritedTreeNode() : BaseInheritedNode() {
        var id: Long? = null
        var parentId: Long? = null
        var name: String? = null
        var children: List<InheritedTreeNode>? = null

        constructor(
            id: Long?,
            parentId: Long?,
            name: String?,
            inheritedName: String?,
            children: List<InheritedTreeNode>?,
        ) : this() {
            this.id = id
            this.parentId = parentId
            this.name = name
            this.inheritedName = inheritedName
            this.children = children
        }

        companion object {
            @JvmField
            var staticCounter: Int = 0
        }
    }

    private class NoDefaultConstructorNode(
        val id: Long,
        val name: String,
    ) {
        var children: List<NoDefaultConstructorNode>? = null
    }
}
