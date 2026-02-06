package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/// OAuth 注册客户端 Repository 测试
///
/// 验证根据 clientId 查询客户端配置的能力，依赖 Liquibase 初始化的数据
///
/// @author lishangbu
/// @since 2025/8/20
class OauthRegisteredClientRepositoryTest extends AbstractRepositoryTest {

  @Resource private Oauth2RegisteredClientRepository oauth2RegisteredClientRepository;

  @Test
  void testFindByClientId() {
    Optional<OauthRegisteredClient> clientOptional =
        oauth2RegisteredClientRepository.findByClientId("client");
    Assertions.assertTrue(clientOptional.isPresent());
    OauthRegisteredClient oauthRegisteredClient = clientOptional.get();
    Assertions.assertEquals("1", oauthRegisteredClient.getId());
    Assertions.assertEquals("client", oauthRegisteredClient.getClientId());
    Assertions.assertEquals("{noop}client", oauthRegisteredClient.getClientSecret());
  }
}
