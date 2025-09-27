package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.authorization.model.PermissionTreeNode;
import io.github.lishangbu.avalon.authorization.service.PermissionService;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限服务控制器
 *
 * @author lishangbu
 * @since 2025/8/28
 */
@Slf4j
@RequestMapping("/permission")
@RestController
@RequiredArgsConstructor
public class PermissionController {
  private final PermissionService permissionService;

  @GetMapping("/role-tree")
  public List<PermissionTreeNode> listPermissionTree(@AuthenticationPrincipal UserInfo user) {
    return permissionService.listPermissionTreeByRoleCodes(
        user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
  }
}
