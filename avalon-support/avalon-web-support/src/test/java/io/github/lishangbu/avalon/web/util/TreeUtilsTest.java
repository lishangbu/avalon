package io.github.lishangbu.avalon.web.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 树形结构工具类单元测试
 *
 * @author lishangbu
 * @since 2025/08/25
 */
class TreeUtilsTest {

  private List<TreeNode> flatNodes;
  private List<TreeNode> treeNodes;

  @BeforeEach
  void setUp() {
    // 准备扁平节点列表
    flatNodes = new ArrayList<>();
    flatNodes.add(new TreeNode(1L, null, "根节点1", null));
    flatNodes.add(new TreeNode(2L, null, "根节点2", null));
    flatNodes.add(new TreeNode(3L, 1L, "子节点1-1", null));
    flatNodes.add(new TreeNode(4L, 1L, "子节点1-2", null));
    flatNodes.add(new TreeNode(5L, 2L, "子节点2-1", null));
    flatNodes.add(new TreeNode(6L, 3L, "子节点1-1-1", null));
    flatNodes.add(new TreeNode(7L, 3L, "子节点1-1-2", null));

    // 准备树形结构
    TreeNode node6 = new TreeNode(6L, 3L, "子节点1-1-1", new ArrayList<>());
    TreeNode node7 = new TreeNode(7L, 3L, "子节点1-1-2", new ArrayList<>());

    TreeNode node3 = new TreeNode(3L, 1L, "子节点1-1", Arrays.asList(node6, node7));
    TreeNode node4 = new TreeNode(4L, 1L, "子节点1-2", new ArrayList<>());
    TreeNode node5 = new TreeNode(5L, 2L, "子节点2-1", new ArrayList<>());

    TreeNode node1 = new TreeNode(1L, null, "根节点1", Arrays.asList(node3, node4));
    TreeNode node2 = new TreeNode(2L, null, "根节点2", Collections.singletonList(node5));

    treeNodes = Arrays.asList(node1, node2);
  }

  @Test
  void buildTree_shouldCreateCorrectTreeStructure() {
    // 使用TreeUtils构建树
    List<TreeNode> result =
        TreeUtils.buildTree(
            flatNodes, TreeNode::getId, TreeNode::getParentId, TreeNode::setChildren);

    // 验证结果
    assertEquals(2, result.size(), "应该有两个根节点");

    TreeNode root1 = result.get(0);
    assertEquals(1L, root1.getId());
    assertEquals(2, root1.getChildren().size(), "根节点1应该有两个子节点");

    TreeNode child1 = root1.getChildren().get(0);
    assertEquals(3L, child1.getId());
    assertEquals(2, child1.getChildren().size(), "子节点1-1应该有两个子节点");

    TreeNode root2 = result.get(1);
    assertEquals(2L, root2.getId());
    assertEquals(1, root2.getChildren().size(), "根节点2应该有一个子节点");
  }

  @Test
  void findNode_shouldReturnCorrectNode() {
    // 查找名称为"子节点1-1"的节点
    TreeNode result =
        TreeUtils.findNode(
            treeNodes, node -> "子节点1-1".equals(node.getName()), TreeNode::getChildren);

    // 验证结果
    assertNotNull(result);
    assertEquals(3L, result.getId());
    assertEquals("子节点1-1", result.getName());
  }

  @Test
  void findNode_shouldReturnNullWhenNotFound() {
    // 查找不存在的节点
    TreeNode result =
        TreeUtils.findNode(
            treeNodes, node -> "不存在的节点".equals(node.getName()), TreeNode::getChildren);

    // 验证结果
    assertNull(result);
  }

  @Test
  void findNodes_shouldReturnAllMatchingNodes() {
    // 查找所有包含"子节点"的节点
    List<TreeNode> result =
        TreeUtils.findNodes(
            treeNodes,
            node -> node.getName() != null && node.getName().contains("子节点"),
            TreeNode::getChildren);

    // 验证结果
    assertEquals(5, result.size(), "应该找到5个包含'子节点'的节点");
  }

  @Test
  void getNodePath_shouldReturnCorrectPath() {
    // 获取到"子节点1-1-1"的路径
    List<TreeNode> result =
        TreeUtils.getNodePath(
            treeNodes, node -> Objects.equals(node.getId(), 6L), TreeNode::getChildren);

    // 验证结果
    assertEquals(3, result.size(), "路径应该包含3个节点");
    assertEquals(1L, result.get(0).getId(), "路径应该从根节点开始");
    assertEquals(3L, result.get(1).getId(), "路径的第二个节点应该是子节点1-1");
    assertEquals(6L, result.get(2).getId(), "路径的最后一个节点应该是目标节点");
  }

  @Test
  void getNodePath_shouldReturnEmptyListWhenNotFound() {
    // 获取不存在节点的路径
    List<TreeNode> result =
        TreeUtils.getNodePath(
            treeNodes, node -> Objects.equals(node.getId(), 999L), TreeNode::getChildren);

    // 验证结果
    assertTrue(result.isEmpty(), "未找到目标节点时应返回空列表");
  }

  @Test
  void flattenTree_shouldReturnFlatList() {
    // 扁平化树结构
    List<TreeNode> result = TreeUtils.flattenTree(treeNodes, TreeNode::getChildren);

    // 验证结果
    assertEquals(7, result.size(), "扁平化后应该有7个节点");

    // 验证是否包含所有节点
    assertTrue(result.stream().anyMatch(node -> node.getId() == 1L));
    assertTrue(result.stream().anyMatch(node -> node.getId() == 3L));
    assertTrue(result.stream().anyMatch(node -> node.getId() == 6L));
  }

  @Test
  void flattenTree_shouldHandleEmptyTree() {
    // 扁平化空树
    List<TreeNode> result = TreeUtils.flattenTree(Collections.emptyList(), TreeNode::getChildren);

    // 验证结果
    assertTrue(result.isEmpty(), "扁平化空树应返回空列表");
  }

  @Test
  void filterTree_shouldFilterCorrectly() {
    // 过滤树，只保留名称中包含"1"的节点及其父节点
    List<TreeNode> result =
        TreeUtils.filterTree(
            treeNodes,
            node -> node.getName() != null && node.getName().contains("1"),
            TreeNode::getChildren,
            TreeNode::setChildren);

    // 验证结果
    assertFalse(result.isEmpty(), "结果不应为空");
    // 根节点1应该被保留，因为它的名字包含"1"
    assertTrue(result.stream().anyMatch(node -> node.getId() == 1L));
    // 检查子节点结构是否正确
    TreeNode root1 = result.stream().filter(n -> n.getId() == 1L).findFirst().orElse(null);
    assertNotNull(root1);
    assertEquals(2, root1.getChildren().size(), "过滤后的根节点1应该保留两个子节点");
  }

  @Test
  void filterTree_shouldHandleNullChildren() {
    // 准备一个没有子节点的树
    List<TreeNode> nodesWithoutChildren = new ArrayList<>();
    nodesWithoutChildren.add(new TreeNode(1L, null, "根节点1", null));

    // 过滤树
    List<TreeNode> result =
        TreeUtils.filterTree(
            nodesWithoutChildren, node -> true, TreeNode::getChildren, TreeNode::setChildren);

    // 验证结果
    assertEquals(1, result.size(), "结果应该包含一个节点");
    assertNull(result.get(0).getChildren(), "节点的子节点应该为null");
  }

  @Test
  void traverseTree_shouldVisitAllNodes() {
    // 用来计数的数组
    final int[] count = {0};
    final List<String> visitedNodeNames = new ArrayList<>();

    // 遍历树，对每个节点执行操作
    TreeUtils.traverseTree(
        treeNodes,
        (node, level) -> {
          count[0]++;
          visitedNodeNames.add(node.getName());
        },
        TreeNode::getChildren);

    // 验证结果
    assertEquals(7, count[0], "应该遍历7个节点");
    assertEquals(7, visitedNodeNames.size(), "应该访问7个节点名称");
    assertTrue(visitedNodeNames.contains("根节点1"), "应该访问根节点1");
    assertTrue(visitedNodeNames.contains("子节点1-1-1"), "应该访问最深层的节点");
  }

  @Test
  void getMaxDepth_shouldReturnCorrectDepth() {
    // 获取树的最大深度
    int result = TreeUtils.getMaxDepth(treeNodes, TreeNode::getChildren);

    // 验证结果
    assertEquals(3, result, "树的最大深度应该是3");
  }

  @Test
  void getMaxDepth_shouldReturnZeroForEmptyTree() {
    // 获取空树的最大深度
    int result = TreeUtils.getMaxDepth(Collections.emptyList(), TreeNode::getChildren);

    // 验证结果
    assertEquals(0, result, "空树的最大深度应该是0");
  }

  @Test
  void findNodeById_shouldReturnCorrectNode() {
    // 根据ID查找节点
    TreeNode result = TreeUtils.findNodeById(treeNodes, 6L, TreeNode::getId, TreeNode::getChildren);

    // 验证结果
    assertNotNull(result);
    assertEquals(6L, result.getId());
    assertEquals("子节点1-1-1", result.getName());
  }

  @Test
  void findNodeById_shouldReturnNullWhenNotFound() {
    // 根据不存在的ID查找节点
    TreeNode result =
        TreeUtils.findNodeById(treeNodes, 999L, TreeNode::getId, TreeNode::getChildren);

    // 验证结果
    assertNull(result, "查找不存在的ID应返回null");
  }

  /** 测试用的树节点类 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class TreeNode {
    private Long id;
    private Long parentId;
    private String name;
    private List<TreeNode> children;
  }
}
