package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.authorization.TestEnvironmentApplication;
import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 单元测试 OauthAuthorizationConsentMapper 的 CRUD 行为
 *
 * <p>使用内存数据库通过 JdbcTemplate 初始化表结构和测试数据，测试每个方法的基本行为
 */
@Testcontainers
@ContextConfiguration(classes = TestEnvironmentApplication.class)
@MybatisPlusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OauthAuthorizationConsentMapperTest {

  @Container @ServiceConnection
  static PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:18");

  @Resource private OauthAuthorizationConsentMapper oauthAuthorizationConsentMapper;

  @Test
  @Order(1)
  @Commit
  void testInsertAndSelect() {
    OauthAuthorizationConsent consent = new OauthAuthorizationConsent();
    consent.setRegisteredClientId("client-1");
    consent.setPrincipalName("user-1");
    consent.setAuthorities("scope1,scope2");

    int inserted = oauthAuthorizationConsentMapper.insert(consent);
    Assertions.assertEquals(1, inserted);

    OauthAuthorizationConsent found =
        oauthAuthorizationConsentMapper.selectByRegisteredClientIdAndPrincipalName(
            "client-1", "user-1");
    Assertions.assertNotNull(found);
    Assertions.assertEquals("client-1", found.getRegisteredClientId());
    Assertions.assertEquals("user-1", found.getPrincipalName());
    Assertions.assertEquals("scope1,scope2", found.getAuthorities());
  }

  @Test
  @Order(2)
  @Commit
  void testUpdateById() {
    OauthAuthorizationConsent toUpdate = new OauthAuthorizationConsent();
    toUpdate.setRegisteredClientId("client-1");
    toUpdate.setPrincipalName("user-1");
    toUpdate.setAuthorities("c,d,e");

    int updated = oauthAuthorizationConsentMapper.updateById(toUpdate);
    Assertions.assertEquals(1, updated);

    OauthAuthorizationConsent found =
        oauthAuthorizationConsentMapper.selectByRegisteredClientIdAndPrincipalName(
            "client-1", "user-1");
    Assertions.assertNotNull(found);
    Assertions.assertEquals("c,d,e", found.getAuthorities());
  }

  @Test
  @Order(3)
  @Commit
  void testDeleteByRegisteredClientIdAndPrincipalName() {
    int deleted =
        oauthAuthorizationConsentMapper.deleteByRegisteredClientIdAndPrincipalName(
            "client-1", "user-1");
    Assertions.assertEquals(1, deleted);

    OauthAuthorizationConsent found =
        oauthAuthorizationConsentMapper.selectByRegisteredClientIdAndPrincipalName(
            "client-1", "user-1");
    Assertions.assertNull(found);
  }
}
