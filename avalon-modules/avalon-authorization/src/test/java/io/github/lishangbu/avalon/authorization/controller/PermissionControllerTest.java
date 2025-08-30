package io.github.lishangbu.avalon.authorization.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.lishangbu.avalon.authorization.entity.Permission;
import io.github.lishangbu.avalon.authorization.service.PermissionService;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PermissionController.class)
@WithMockUser(
    username = "testuser",
    roles = {"ADMIN"})
@ContextConfiguration(classes = PermissionControllerTest.TestConfig.class)
class PermissionControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private PermissionService permissionService;

  @Test
  void listPermissions_shouldReturnList() throws Exception {
    Permission p = new Permission();
    p.setName("测试权限");
    when(permissionService.listPermissions(any(Permission.class))).thenReturn(List.of(p));

    mockMvc
        .perform(get("/permission/list"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("测试权限"));

    verify(permissionService, times(1)).listPermissions(any(Permission.class));
  }

  @Test
  void listPermissionTree_root_shouldReturnEmptyList() throws Exception {
    when(permissionService.listPermissionTreeNodes(null)).thenReturn(List.of());

    mockMvc
        .perform(get("/permission/tree"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    verify(permissionService, times(1)).listPermissionTreeNodes(null);
  }

  @Test
  void menuTree_withQuery_shouldCallService() throws Exception {
    when(permissionService.listPermissionTreeNodes(any(Permission.class))).thenReturn(List.of());

    mockMvc
        .perform(get("/permission/menu-tree"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    verify(permissionService, times(1)).listPermissionTreeNodes(any(Permission.class));
  }

  @Test
  void rolePermissionTree_withAuthentication_shouldPassRoleCodes() throws Exception {
    UserInfo user = new UserInfo("1", "testuser", "password", "ADMIN,USER");

    when(permissionService.listPermissionTreeByRoleCodes(Arrays.asList("ADMIN", "USER")))
        .thenReturn(List.of());

    mockMvc
        .perform(get("/permission/role-permission-tree").with(user(user)))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    verify(permissionService, times(1))
        .listPermissionTreeByRoleCodes(Arrays.asList("ADMIN", "USER"));
  }

  @SpringBootConfiguration
  @Import(PermissionController.class)
  static class TestConfig {
    @Bean
    public PermissionService permissionService() {
      return mock(PermissionService.class);
    }
  }
}
