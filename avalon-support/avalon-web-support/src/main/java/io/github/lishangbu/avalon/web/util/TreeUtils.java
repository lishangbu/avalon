package io.github.lishangbu.avalon.web.util;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/// 树形结构工具类，用于处理树形数据
///
/// 提供将列表转换为树、查找节点、扁平化、过滤和遍历等常用操作
///
/// @author lishangbu
/// @since 2025/08/25
@Slf4j
public class TreeUtils {

  /// 将列表转换为树形结构
  ///
  /// @param list           待转换的列表
  /// @param idGetter       获取节点ID的函数
  /// @param parentIdGetter 获取父节点ID的函数
  /// @param childrenSetter 设置子节点的函数
  /// @param <T>            节点类型
  /// @param <I>            ID类型
  /// @return 树形结构的根节点列表
  public static <T, I> List<T> buildTree(
      List<T> list,
      Function<T, I> idGetter,
      Function<T, I> parentIdGetter,
      BiConsumer<T, List<T>> childrenSetter) {
    if (list == null || list.isEmpty()) {
      return Collections.emptyList();
    }

    // 构建节点映射表 (ID -> 节点)
    Map<I, T> nodeMap = list.stream().collect(Collectors.toMap(idGetter, node -> node));

    List<T> roots = new ArrayList<>();

    // 遍历所有节点，将它们添加到父节点的子节点列表中
    for (T node : list) {
      I parentId = parentIdGetter.apply(node);

      if (parentId == null) {
        // 如果父ID为null，则为根节点
        roots.add(node);
      } else {
        // 尝试获取父节点
        T parentNode = nodeMap.get(parentId);
        if (parentNode != null) {
          // 获取父节点的子节点列表，如果不存在则创建新列表
          List<T> children = getChildren(parentNode, childrenSetter);
          if (children == null) {
            children = new ArrayList<>();
            childrenSetter.accept(parentNode, children);
          }
          children.add(node);
        } else {
          // 找不到父节点，作为根节点处理
          roots.add(node);
        }
      }
    }

    return roots;
  }

  /// 获取节点的子节点列表。注意：这是一个辅助方法，尝试通过反射调用 `getChildren()`
  @SuppressWarnings("unchecked")
  private static <T> List<T> getChildren(T node, BiConsumer<T, List<T>> childrenSetter) {
    // 这里需要根据实际情况实现，一个常见方式是节点类有getChildren方法
    // 这里使用反射模拟获取children字段
    try {
      return (List<T>) node.getClass().getMethod("getChildren").invoke(node);
    } catch (Exception e) {
      return null;
    }
  }

  /// 在树中查找符合条件的第一个节点
  ///
  /// @param tree           树结构
  /// @param predicate      匹配条件
  /// @param childrenGetter 获取子节点的函数
  /// @param <T>            节点类型
  /// @return 找到的节点，没有则返回 null
  public static <T> T findNode(
      List<T> tree, Predicate<T> predicate, Function<T, List<T>> childrenGetter) {
    if (tree == null || tree.isEmpty()) {
      return null;
    }

    for (T node : tree) {
      // 检查当前节点
      if (predicate.test(node)) {
        return node;
      }

      // 递归检查子节点
      List<T> children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        T found = findNode(children, predicate, childrenGetter);
        if (found != null) {
          return found;
        }
      }
    }

    return null;
  }

  /// 在树中查找所有符合条件的节点
  ///
  /// @param tree           树结构
  /// @param predicate      匹配条件
  /// @param childrenGetter 获取子节点的函数
  /// @param <T>            节点类型
  /// @return 找到的节点列表
  public static <T> List<T> findNodes(
      List<T> tree, Predicate<T> predicate, Function<T, List<T>> childrenGetter) {
    List<T> result = new ArrayList<>();
    findNodesInternal(tree, predicate, childrenGetter, result);
    return result;
  }

  /// 查找节点的内部递归方法
  private static <T> void findNodesInternal(
      List<T> nodes, Predicate<T> predicate, Function<T, List<T>> childrenGetter, List<T> result) {
    if (nodes == null || nodes.isEmpty()) {
      return;
    }

    for (T node : nodes) {
      // 检查当前节点
      if (predicate.test(node)) {
        result.add(node);
      }

      // 递归检查子节点
      List<T> children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        findNodesInternal(children, predicate, childrenGetter, result);
      }
    }
  }

  /// 获取从根节点到目标节点的路径
  /// @param tree 树结构
  /// @param targetPredicate 目标节点匹配条件
  /// @param childrenGetter 获取子节点的函数
  /// @param <T> 节点类型
  /// @return 路径节点列表，如果找不到目标节点则返回空列表
  public static <T> List<T> getNodePath(
      List<T> tree, Predicate<T> targetPredicate, Function<T, List<T>> childrenGetter) {
    List<T> path = new ArrayList<>();
    findPath(tree, targetPredicate, childrenGetter, path);
    return path;
  }

  /// 查找路径的内部递归方法
  private static <T> boolean findPath(
      List<T> nodes,
      Predicate<T> targetPredicate,
      Function<T, List<T>> childrenGetter,
      List<T> path) {
    if (nodes == null || nodes.isEmpty()) {
      return false;
    }

    for (T node : nodes) {
      // 添加当前节点到路径
      path.add(node);

      // 检查是否找到目标节点
      if (targetPredicate.test(node)) {
        return true;
      }

      // 递归检查子节点
      List<T> children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        if (findPath(children, targetPredicate, childrenGetter, path)) {
          return true;
        }
      }

      // 当前路径不包含目标节点，移除此节点
      path.remove(path.size() - 1);
    }

    return false;
  }

  /// 树形结构扁平化为列表
  /// @param tree 树结构
  /// @param childrenGetter 获取子节点的函数
  /// @param <T> 节点类型
  /// @return 扁平化后的列表
  public static <T> List<T> flattenTree(List<T> tree, Function<T, List<T>> childrenGetter) {
    List<T> result = new ArrayList<>();
    flattenTreeInternal(tree, childrenGetter, result);
    return result;
  }

  /// 过滤树节点，保持树形结构
  /// @param tree 树结构
  /// @param predicate 过滤条件
  /// @param childrenGetter 获取子节点的函数
  /// @param childrenSetter 设置子节点的函数
  /// @param <T> 节点类型
  /// @return 过滤后的树结构
  public static <T> List<T> filterTree(
      List<T> tree,
      Predicate<T> predicate,
      Function<T, List<T>> childrenGetter,
      BiConsumer<T, List<T>> childrenSetter) {
    if (tree == null || tree.isEmpty()) {
      return Collections.emptyList();
    }

    List<T> result = new ArrayList<>();

    for (T node : tree) {
      // 深拷贝节点以避免修改原树
      T copyNode;
      try {
        // 首先创建新实例
        copyNode = (T) node.getClass().getDeclaredConstructor().newInstance();

        // 复制所有非静态字段的值
        for (java.lang.reflect.Field field : getAllFields(node.getClass())) {
          // 跳过静态字段
          if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            continue;
          }

          field.setAccessible(true);
          Object value = field.get(node);

          // 不复制children字段，这个字段会由childrenSetter单独处理
          if (!field.getName().equals("children")) {
            field.set(copyNode, value);
          }
        }
      } catch (Exception e) {
        log.error("Failed to create or copy node instance: {}", e.getMessage());
        copyNode = node; // 如果无法创建新实例，则使用原节点
      }

      // 处理子节点
      List<T> children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        List<T> filteredChildren = filterTree(children, predicate, childrenGetter, childrenSetter);
        childrenSetter.accept(copyNode, filteredChildren);
      } else {
        // 确保copyNode的子节点列表为null或空列表
        childrenSetter.accept(copyNode, children == null ? null : Collections.emptyList());
      }

      // 如果当前节点满足条件，或者它有满足条件的子节点，则添加到结果中
      List<T> copyNodeChildren = childrenGetter.apply(copyNode);
      if (predicate.test(node) || (copyNodeChildren != null && !copyNodeChildren.isEmpty())) {
        result.add(copyNode);
      }
    }

    return result;
  }

  /// 遍历树结构，对每个节点执行操作
  /// @param tree 树结构
  /// @param action 要执行的操作
  /// @param childrenGetter 获取子节点的函数
  /// @param <T> 节点类型
  public static <T> void traverseTree(
      List<T> tree, BiConsumer<T, Integer> action, Function<T, List<T>> childrenGetter) {
    traverseTreeInternal(tree, action, childrenGetter, 0);
  }

  /// 计算树的最大深度
  /// @param tree 树结构
  /// @param childrenGetter 获取子节点的函数
  /// @param <T> 节点类型
  /// @return 树的最大深度
  public static <T> int getMaxDepth(List<T> tree, Function<T, List<T>> childrenGetter) {
    if (tree == null || tree.isEmpty()) {
      return 0;
    }

    int maxDepth = 0;

    for (T node : tree) {
      List<T> children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        int depth = getMaxDepth(children, childrenGetter);
        maxDepth = Math.max(maxDepth, depth);
      }
    }

    return maxDepth + 1; // 加上当前层
  }

  /// 根据节点ID查找节点
  /// @param tree 树结构
  /// @param id 目标节点ID
  /// @param idGetter 获取节点ID的函数
  /// @param childrenGetter 获取子节点的函数
  /// @param <T> 节点类型
  /// @param <I> ID类型
  /// @return 找到的节点，没有则返回null
  public static <T, I> T findNodeById(
      List<T> tree, I id, Function<T, I> idGetter, Function<T, List<T>> childrenGetter) {
    return findNode(tree, node -> Objects.equals(idGetter.apply(node), id), childrenGetter);
  }

  /// 获取类的所有字段，包括继承的字段
  private static List<java.lang.reflect.Field> getAllFields(Class<?> clazz) {
    List<java.lang.reflect.Field> fields = new ArrayList<>();

    // 获取当前类的字段
    fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

    // 递归获取父类的字段
    Class<?> superClass = clazz.getSuperclass();
    if (superClass != null && !superClass.equals(Object.class)) {
      fields.addAll(getAllFields(superClass));
    }

    return fields;
  }

  /// 遍历树的内部递归方法
  private static <T> void traverseTreeInternal(
      List<T> nodes,
      BiConsumer<T, Integer> action,
      Function<T, List<T>> childrenGetter,
      int level) {
    if (nodes == null || nodes.isEmpty()) {
      return;
    }

    for (T node : nodes) {
      // 对当前节点执行操作
      action.accept(node, level);

      // 递归处理子节点
      List<T> children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        traverseTreeInternal(children, action, childrenGetter, level + 1);
      }
    }
  }

  /// 扁平化树的内部递归方法
  private static <T> void flattenTreeInternal(
      List<T> nodes, Function<T, List<T>> childrenGetter, List<T> result) {
    if (nodes == null || nodes.isEmpty()) {
      return;
    }

    for (T node : nodes) {
      result.add(node);

      List<T> children = childrenGetter.apply(node);
      if (children != null && !children.isEmpty()) {
        flattenTreeInternal(children, childrenGetter, result);
      }
    }
  }
}
