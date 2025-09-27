package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.repository.UserRepository;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户服务
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Service
@RequiredArgsConstructor
public class DefaultUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository
        .findUserWithRolesByUsername(username)
        .map(
            user ->
                new UserInfo(
                    user.getUsername(),
                    user.getPassword(),
                    user.getRoles() == null
                        ? null
                        : AuthorityUtils.createAuthorityList(
                            user.getRoles().stream().map(Role::getCode).toList())))
        .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));
  }
}
