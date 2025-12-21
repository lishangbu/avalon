package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.authorization.TestEnvironmentApplication;
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * OAuth注册客户端Mapper测试类
 *
 * <p>使用Testcontainers启动PostgreSQL容器进行集成测试
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@Testcontainers
@ContextConfiguration(classes = TestEnvironmentApplication.class)
@MybatisPlusTest
class OauthRegisteredClientMapperTest {
  @Container @ServiceConnection
  static PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:latest");

  @Resource private OauthRegisteredClientMapper oauth2RegisteredClientMapper;

  @Test
  void testSelectByClientId() {
    Optional<OauthRegisteredClient> clientOptional =
        oauth2RegisteredClientMapper.selectByClientId("client");
    Assertions.assertTrue(clientOptional.isPresent());
    OauthRegisteredClient oauthRegisteredClient = clientOptional.get();
    Assertions.assertNotNull(oauthRegisteredClient);
    Assertions.assertEquals("1", oauthRegisteredClient.getId());
    Assertions.assertEquals("client", oauthRegisteredClient.getClientId());
    Assertions.assertEquals("{noop}client", oauthRegisteredClient.getClientSecret());
  }
}
