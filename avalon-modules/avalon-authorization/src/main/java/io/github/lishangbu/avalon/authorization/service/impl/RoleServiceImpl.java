package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.Menu;
import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.entity.Role_;
import io.github.lishangbu.avalon.authorization.repository.MenuRepository;
import io.github.lishangbu.avalon.authorization.repository.RoleRepository;
import io.github.lishangbu.avalon.authorization.service.RoleService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/// 角色信息服务实现类
///
/// 提供对角色数据的查询与管理（实现 RoleService）
///
/// @author lishangbu
/// @since 2025/8/30
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    @Override
    public Page<Role> getPageByCondition(Role role, Pageable pageable) {
        return roleRepository.findAll(
                Example.of(
                        role,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        Role_.CODE,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        Role_.NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())),
                pageable);
    }

    @Override
    public List<Role> listByCondition(Role role) {
        return roleRepository.findAll(
                Example.of(
                        role,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        Role_.CODE,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        Role_.NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())));
    }

    @Override
    public Optional<Role> getById(Long id) {
        return roleRepository.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role save(Role role) {
        bindMenus(role, false);
        return roleRepository.save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role update(Role role) {
        bindMenus(role, true);
        return roleRepository.save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        roleRepository.deleteById(id);
    }

    private void bindMenus(Role role, boolean preserveWhenNull) {
        if (role == null) {
            return;
        }
        if (CollectionUtils.isEmpty(role.getMenus())) {
            if (preserveWhenNull && role.getId() != null) {
                roleRepository
                        .findById(role.getId())
                        .ifPresent(existing -> role.setMenus(existing.getMenus()));
            }
            return;
        }
        Set<Long> menuIds =
                role.getMenus().stream()
                        .map(Menu::getId)
                        .filter(id -> id != null)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (menuIds.isEmpty()) {
            role.setMenus(Set.of());
            return;
        }
        List<Menu> boundMenus = menuRepository.findAllById(menuIds);
        role.setMenus(new LinkedHashSet<>(boundMenus));
    }
}
