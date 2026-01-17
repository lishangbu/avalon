package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.authorization.AbstractMapperTest;
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/// OAuth 注册客户端数据访问层测试类
///
/// 测试 OauthRegisteredClientMapper 的基本查询操作，继承 AbstractMapperTest 以复用 PostgreSQL 容器实例
///
/// @author lishangbu
/// @since 2025/8/20
@MybatisPlusTest
class OauthRegisteredClientMapperTest extends AbstractMapperTest {

  @Resource private OauthRegisteredClientMapper oauth2RegisteredClientMapper;

  /// 测试根据客户端ID查询注册客户端
  ///
  /// 验证能够通过客户端ID正确查询到注册客户端信息
  @Test
  void shouldFindRegisteredClientByClientId() {
    // Act
    Optional<OauthRegisteredClient> clientOptional =
        oauth2RegisteredClientMapper.selectByClientId("client");

    // Assert
    Assertions.assertTrue(clientOptional.isPresent());
    OauthRegisteredClient oauthRegisteredClient = clientOptional.get();
    Assertions.assertNotNull(oauthRegisteredClient);
    Assertions.assertEquals("1", oauthRegisteredClient.getId());
    Assertions.assertEquals("client", oauthRegisteredClient.getClientId());
    Assertions.assertEquals("{noop}client", oauthRegisteredClient.getClientSecret());
  }
}
