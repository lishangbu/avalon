package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.authorization.model.MenuTreeNode;
import io.github.lishangbu.avalon.authorization.service.MenuService;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜单控制器
 *
 * @author lishangbu
 * @since 2025/8/28
 */
@Slf4j
@RequestMapping("/menu")
@RestController
@RequiredArgsConstructor
public class MenuController {
  private final MenuService menuService;

  @GetMapping("/role-tree")
  public List<MenuTreeNode> listCurrentRoleMenuTree(@AuthenticationPrincipal UserInfo user) {
    return menuService.listMenuTreeByRoleCodes(
        Arrays.stream(user.getRoleCodes().split(",")).toList());
  }
}
