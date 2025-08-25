package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.MapperTestEnvironmentAutoConfiguration;
import io.github.lishangbu.avalon.authorization.entity.Oauth2RegisteredClient;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@ContextConfiguration(classes = {MapperTestEnvironmentAutoConfiguration.class})
@MybatisTest
class Oauth2RegisteredClientMapperTest {

  @Resource private Oauth2RegisteredClientMapper oauth2RegisteredClientMapper;

  @Test
  void testSelectByClientId() {
    Optional<Oauth2RegisteredClient> clientOptional =
        oauth2RegisteredClientMapper.selectByClientId("client");
    Assertions.assertTrue(clientOptional.isPresent());
    Oauth2RegisteredClient oauth2RegisteredClient = clientOptional.get();
    Assertions.assertEquals("1", oauth2RegisteredClient.getId());
    Assertions.assertEquals("client", oauth2RegisteredClient.getClientId());
    Assertions.assertEquals("{noop}client", oauth2RegisteredClient.getClientSecret());
  }
}
