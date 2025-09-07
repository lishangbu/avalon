package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.TestEnvironmentAutoConfiguration;
import io.github.lishangbu.avalon.authorization.entity.Permission;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/8/30
 */
@ContextConfiguration(classes = {TestEnvironmentAutoConfiguration.class})
@DataJpaTest
public class PermissionRepositoryTest {
  @Resource private PermissionRepository permissionRepository;

  @Test
  public void testFindAll() {
    List<String> roleCodes = new ArrayList<>();
    roleCodes.add("ROLE_SUPER_ADMIN");
    List<Permission> permissions = permissionRepository.findAllByRoleCodes(roleCodes);
    System.out.println(permissions);
  }
}
