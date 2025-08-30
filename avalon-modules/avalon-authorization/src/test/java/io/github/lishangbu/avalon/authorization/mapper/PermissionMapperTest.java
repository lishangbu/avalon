package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.MapperTestEnvironmentAutoConfiguration;
import io.github.lishangbu.avalon.authorization.entity.Permission;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/8/30
 */
@ContextConfiguration(classes = {MapperTestEnvironmentAutoConfiguration.class})
@MybatisTest
public class PermissionMapperTest {
  @Resource private PermissionMapper permissionMapper;

  @Test
  public void testSelectAll() {
    List<String> roleCodes = new ArrayList<>();
    roleCodes.add("ROLE_SUPER_ADMIN");
    List<Permission> permissions = permissionMapper.selectAllByRoleCodes(roleCodes);
    System.out.println(permissions);
  }
}
