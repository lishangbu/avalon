package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.authorization.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色控制器
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@RequestMapping("/role")
@RestController
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;
}
