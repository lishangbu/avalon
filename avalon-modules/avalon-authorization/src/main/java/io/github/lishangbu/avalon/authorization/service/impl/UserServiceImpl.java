package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.entity.User_;
import io.github.lishangbu.avalon.authorization.repository.RoleRepository;
import io.github.lishangbu.avalon.authorization.model.UserWithRoles;
import io.github.lishangbu.avalon.authorization.repository.UserRepository;
import io.github.lishangbu.avalon.authorization.service.UserService;
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

/// 用户服务实现
///
/// @author lishangbu
/// @since 2025/8/30
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /// 根据用户名/手机号/邮箱查询用户详情，包含基本信息、角色信息及个人资料
    ///
    /// @param username 登录账号
    /// @return 查询到的用户详情，未找到时返回Optional.empty()
    @Override
    public Optional<UserWithRoles> getUserByUsername(String username) {
        return userRepository.findUserWithRolesByAccount(username).map(UserWithRoles::new);
    }

    @Override
    public Page<User> getPageByCondition(User user, Pageable pageable) {
        return userRepository.findAll(
                Example.of(
                        user,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        User_.USERNAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        User_.PHONE,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        User_.EMAIL,
                                        ExampleMatcher.GenericPropertyMatchers.contains())),
                pageable);
    }

    @Override
    public List<User> listByCondition(User user) {
        return userRepository.findAll(
                Example.of(
                        user,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        User_.USERNAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        User_.PHONE,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        User_.EMAIL,
                                        ExampleMatcher.GenericPropertyMatchers.contains())));
    }

    @Override
    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User save(User user) {
        bindRoles(user, false);
        return userRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User update(User user) {
        bindRoles(user, true);
        return userRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        userRepository.deleteById(id);
    }

    private void bindRoles(User user, boolean preserveWhenNull) {
        if (user == null) {
            return;
        }
        if (CollectionUtils.isEmpty(user.getRoles())) {
            if (preserveWhenNull && user.getId() != null) {
                userRepository
                        .findById(user.getId())
                        .ifPresent(
                                existing -> {
                                    if (user.getHashedPassword() == null) {
                                        user.setHashedPassword(existing.getHashedPassword());
                                    }
                                    user.setRoles(existing.getRoles());
                                });
            }
            return;
        }
        Set<Long> roleIds =
                user.getRoles().stream().map(Role::getId).filter(id -> id != null).collect(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            user.setRoles(Set.of());
            return;
        }
        List<Role> boundRoles = roleRepository.findAllById(roleIds);
        user.setRoles(new LinkedHashSet<>(boundRoles));
    }
}
