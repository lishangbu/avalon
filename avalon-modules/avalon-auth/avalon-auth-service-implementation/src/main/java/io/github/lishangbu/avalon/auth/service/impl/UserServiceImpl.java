package io.github.lishangbu.avalon.auth.service.impl;

import io.github.lishangbu.avalon.auth.entity.Role;
import io.github.lishangbu.avalon.auth.entity.User;
import io.github.lishangbu.avalon.auth.model.SignUpPayload;
import io.github.lishangbu.avalon.auth.repository.RoleRepository;
import io.github.lishangbu.avalon.auth.repository.UserRepository;
import io.github.lishangbu.avalon.auth.service.UserService;
import io.github.lishangbu.avalon.security.core.UserPrincipal;
import java.util.*;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 用户服务
 *
 * @author lishangbu
 * @since 2025/4/9
 */
@Service
public class UserServiceImpl implements UserService, UserDetailsService {
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  public UserServiceImpl(
      PasswordEncoder passwordEncoder,
      UserRepository userRepository,
      RoleRepository roleRepository) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<User> userOptional = userRepository.findByUsername(username);
    if (userOptional.isPresent()) {
      User user = userOptional.get();
      return new UserPrincipal(
          user.getId(),
          username,
          user.getPassword(),
          AuthorityUtils.createAuthorityList(user.getRoles().stream().map(Role::getCode).toList()));
    } else {
      throw new UsernameNotFoundException("用户名或密码错误");
    }
  }

  /**
   * 注册用户
   *
   * @param payload 用户注册数据
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void signUp(SignUpPayload payload) {
    Optional<User> userOptional = userRepository.findByUsername(payload.username());
    Assert.isTrue(userOptional.isEmpty(), "用户已存在");
    User user = new User();
    user.setUsername(payload.username());
    user.setPassword(passwordEncoder.encode(payload.password()));
    roleRepository
        .findByCode(payload.roleCode())
        .ifPresent(
            role -> {
              List<Role> roles = new ArrayList<>();
              roles.add(role);
              user.setRoles(roles);
            });
    userRepository.save(user);
  }
}
