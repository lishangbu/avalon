package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.TestEnvironmentAutoConfiguration;
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/8/20
 */
@ContextConfiguration(classes = {TestEnvironmentAutoConfiguration.class})
@DataJpaTest
class OauthRegisteredClientRepositoryTest {

  @Resource private Oauth2RegisteredClientRepository oauth2RegisteredClientRepository;

  @Test
  void testSelectByClientId() {
    Optional<OauthRegisteredClient> clientOptional =
        oauth2RegisteredClientRepository.findByClientId("client");
    Assertions.assertTrue(clientOptional.isPresent());
    OauthRegisteredClient oauthRegisteredClient = clientOptional.get();
    Assertions.assertEquals("1", oauthRegisteredClient.getId());
    Assertions.assertEquals("client", oauthRegisteredClient.getClientId());
    Assertions.assertEquals("{noop}client", oauthRegisteredClient.getClientSecret());
  }
}
