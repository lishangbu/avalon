package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.Menu;
import io.github.lishangbu.avalon.authorization.model.MenuTreeNode;
import io.github.lishangbu.avalon.authorization.repository.MenuRepository;
import io.github.lishangbu.avalon.authorization.service.MenuService;
import io.github.lishangbu.avalon.web.util.TreeUtils;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/// 菜单服务接口实现
///
/// 提供根据角色代码构建菜单树的实现
///
/// @author lishangbu
/// @since 2025/9/19
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {
    private static final ExampleMatcher MENU_QUERY_MATCHER =
            ExampleMatcher.matching()
                    .withIgnoreNullValues()
                    .withMatcher("icon", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("key", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("label", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("path", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("redirect", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("component", ExampleMatcher.GenericPropertyMatchers.contains());

    private static final Sort MENU_TREE_SORT =
            Sort.by(Sort.Order.asc("sortingOrder"), Sort.Order.asc("id"));

    private final MenuRepository menuRepository;

    @Override
    public List<MenuTreeNode> listMenuTreeByRoleCodes(List<String> roleCodes) {
        if (CollectionUtils.isEmpty(roleCodes)) {
            return Collections.emptyList();
        }
        List<Menu> menus = menuRepository.findAllByRoleCodes(roleCodes);
        log.debug("根据角色编码获取到 [{}] 条菜单记录", menus == null ? 0 : menus.size());
        return buildTreeFromMenus(menus);
    }

    @Override
    public List<MenuTreeNode> listAllMenuTree(Menu menu) {
        Menu condition = normalizeCondition(menu);
        List<Menu> allMenus = menuRepository.findAllByOrderBySortingOrderAscIdAsc();
        if (CollectionUtils.isEmpty(allMenus)) {
            return Collections.emptyList();
        }
        if (!hasQueryCondition(condition)) {
            return buildTreeFromMenus(allMenus);
        }
        List<Menu> matchedMenus =
                menuRepository.findAll(Example.of(condition, MENU_QUERY_MATCHER), MENU_TREE_SORT);
        if (CollectionUtils.isEmpty(matchedMenus)) {
            return Collections.emptyList();
        }
        Map<Long, Menu> menuById =
                allMenus.stream()
                        .filter(menuItem -> menuItem.getId() != null)
                        .collect(Collectors.toMap(Menu::getId, Function.identity()));
        Map<Long, List<Menu>> childrenByParentId =
                allMenus.stream()
                        .filter(menuItem -> menuItem.getId() != null)
                        .collect(Collectors.groupingBy(Menu::getParentId));

        Set<Long> includedIds = new LinkedHashSet<>();
        for (Menu matchedMenu : matchedMenus) {
            if (matchedMenu.getId() == null) {
                continue;
            }
            collectAncestors(menuById, matchedMenu.getId(), includedIds);
            collectDescendants(matchedMenu.getId(), childrenByParentId, includedIds);
        }
        if (includedIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Menu> filteredMenus =
                allMenus.stream()
                        .filter(
                                menuItem ->
                                        menuItem.getId() != null
                                                && includedIds.contains(menuItem.getId()))
                        .toList();
        return buildTreeFromMenus(filteredMenus);
    }

    @Override
    public Optional<Menu> getById(Long id) {
        return menuRepository.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Menu save(Menu menu) {
        return menuRepository.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Menu update(Menu menu) {
        return menuRepository.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        menuRepository.deleteById(id);
    }

    private static void collectAncestors(
            Map<Long, Menu> menuById, Long startId, Set<Long> includedIds) {
        Long currentId = startId;
        while (currentId != null && includedIds.add(currentId)) {
            Menu current = menuById.get(currentId);
            if (current == null) {
                break;
            }
            currentId = current.getParentId();
        }
    }

    private static void collectDescendants(
            Long startId, Map<Long, List<Menu>> childrenByParentId, Set<Long> includedIds) {
        if (startId == null) {
            return;
        }
        ArrayDeque<Long> deque = new ArrayDeque<>();
        Set<Long> visited = new LinkedHashSet<>();
        deque.push(startId);
        while (!deque.isEmpty()) {
            Long currentId = deque.pop();
            if (!visited.add(currentId)) {
                continue;
            }
            includedIds.add(currentId);
            for (Menu child : childrenByParentId.getOrDefault(currentId, Collections.emptyList())) {
                if (child.getId() != null) {
                    deque.push(child.getId());
                }
            }
        }
    }

    private static Menu normalizeCondition(Menu menu) {
        if (menu == null) {
            return null;
        }
        Menu normalized = new Menu();
        normalized.setId(menu.getId());
        normalized.setParentId(menu.getParentId());
        normalized.setDisabled(menu.getDisabled());
        normalized.setExtra(menu.getExtra());
        normalized.setIcon(trimToNull(menu.getIcon()));
        normalized.setKey(trimToNull(menu.getKey()));
        normalized.setLabel(trimToNull(menu.getLabel()));
        normalized.setShow(menu.getShow());
        normalized.setPath(trimToNull(menu.getPath()));
        normalized.setName(trimToNull(menu.getName()));
        normalized.setRedirect(trimToNull(menu.getRedirect()));
        normalized.setComponent(trimToNull(menu.getComponent()));
        normalized.setSortingOrder(menu.getSortingOrder());
        normalized.setPinned(menu.getPinned());
        normalized.setShowTab(menu.getShowTab());
        normalized.setEnableMultiTab(menu.getEnableMultiTab());
        return normalized;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static boolean hasQueryCondition(Menu menu) {
        return menu != null
                && (menu.getId() != null
                        || menu.getParentId() != null
                        || menu.getDisabled() != null
                        || menu.getExtra() != null
                        || menu.getIcon() != null
                        || menu.getKey() != null
                        || menu.getLabel() != null
                        || menu.getShow() != null
                        || menu.getPath() != null
                        || menu.getName() != null
                        || menu.getRedirect() != null
                        || menu.getComponent() != null
                        || menu.getSortingOrder() != null
                        || menu.getPinned() != null
                        || menu.getShowTab() != null
                        || menu.getEnableMultiTab() != null);
    }

    /// 将权限实体列表转换为树节点并构建树结构
    ///
    /// - 处理空集合，返回不可变空列表
    /// - 将 Menu 映射为 MenuTreeNode
    /// - 使用通用的 [TreeUtils] 构建树
    ///
    /// @param menus 权限实体列表，允许为 null
    /// @return 树结构的 MenuTreeNode 列表，永远不返回 null
    /// @see TreeUtils#buildTree(List, Function, Function, BiConsumer)
    private List<MenuTreeNode> buildTreeFromMenus(List<Menu> menus) {
        if (CollectionUtils.isEmpty(menus)) {
            return Collections.emptyList();
        }

        List<MenuTreeNode> treeNodes = menus.stream().map(MenuTreeNode::new).toList();

        return TreeUtils.buildTree(
                treeNodes,
                MenuTreeNode::getId,
                MenuTreeNode::getParentId,
                MenuTreeNode::setChildren);
    }
}
