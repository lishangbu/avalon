package io.github.lishangbu.avalon.authorization.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.core.authority.AuthorityUtils;
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
  void rolePermissionTree_withAuthentication_shouldPassRoleCodes() throws Exception {
    UserInfo user =
        new UserInfo("testuser", "password", AuthorityUtils.createAuthorityList("ADMIN", "USER"));

    when(permissionService.listPermissionTreeByRoleCodes(Arrays.asList("ADMIN", "USER")))
        .thenReturn(List.of());

    mockMvc
        .perform(get("/permission/role-tree").with(user(user)))
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
