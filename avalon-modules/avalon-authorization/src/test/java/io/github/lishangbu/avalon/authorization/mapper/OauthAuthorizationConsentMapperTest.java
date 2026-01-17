package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.authorization.AbstractMapperTest;
import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/// OAuth 授权同意数据访问层测试类
///
/// 测试 OauthAuthorizationConsentMapper 的基本 CRUD 操作，继承 AbstractMapperTest 以复用 PostgreSQL 容器实例
///
/// @author lishangbu
/// @since 2025/8/20
@MybatisPlusTest
class OauthAuthorizationConsentMapperTest extends AbstractMapperTest {

  @Resource private OauthAuthorizationConsentMapper oauthAuthorizationConsentMapper;

  /// 测试插入和查询 OAuth 授权同意记录
  ///
  /// 验证插入操作成功后能够通过客户端ID和主体名称查询到记录
  @Test
  void shouldInsertAndFindConsentByClientIdAndPrincipalName() {
    // Arrange
    OauthAuthorizationConsent consent = new OauthAuthorizationConsent();
    consent.setRegisteredClientId("client-1");
    consent.setPrincipalName("user-1");
    consent.setAuthorities("scope1,scope2");

    // Act
    int inserted = oauthAuthorizationConsentMapper.insert(consent);

    // Assert
    Assertions.assertEquals(1, inserted);

    OauthAuthorizationConsent found =
        oauthAuthorizationConsentMapper.selectByRegisteredClientIdAndPrincipalName(
            "client-1", "user-1");
    Assertions.assertNotNull(found);
    Assertions.assertEquals("client-1", found.getRegisteredClientId());
    Assertions.assertEquals("user-1", found.getPrincipalName());
    Assertions.assertEquals("scope1,scope2", found.getAuthorities());
  }

  /// 测试通过 ID 更新 OAuth 授权同意记录
  ///
  /// 验证更新操作成功后能够通过客户端ID和主体名称查询到最新的记录
  @Test
  void shouldUpdateConsentById() {
    // Arrange - 先插入一条记录用于更新
    OauthAuthorizationConsent consent = new OauthAuthorizationConsent();
    consent.setRegisteredClientId("client-update");
    consent.setPrincipalName("user-update");
    consent.setAuthorities("scope1,scope2");
    oauthAuthorizationConsentMapper.insert(consent);

    // 更新记录
    OauthAuthorizationConsent toUpdate = new OauthAuthorizationConsent();
    toUpdate.setRegisteredClientId("client-update");
    toUpdate.setPrincipalName("user-update");
    toUpdate.setAuthorities("c,d,e");

    // Act
    int updated = oauthAuthorizationConsentMapper.updateById(toUpdate);

    // Assert
    Assertions.assertEquals(1, updated);

    OauthAuthorizationConsent found =
        oauthAuthorizationConsentMapper.selectByRegisteredClientIdAndPrincipalName(
            "client-update", "user-update");
    Assertions.assertNotNull(found);
    Assertions.assertEquals("c,d,e", found.getAuthorities());
  }

  /// 测试通过客户端ID和主体名称删除 OAuth 授权同意记录
  ///
  /// 验证删除操作成功后无法再通过客户端ID和主体名称查询到记录
  @Test
  void shouldDeleteConsentByClientIdAndPrincipalName() {
    // Arrange - 先插入一条记录用于删除
    OauthAuthorizationConsent consent = new OauthAuthorizationConsent();
    consent.setRegisteredClientId("client-delete");
    consent.setPrincipalName("user-delete");
    consent.setAuthorities("scope1,scope2");
    oauthAuthorizationConsentMapper.insert(consent);

    // Act
    int deleted =
        oauthAuthorizationConsentMapper.deleteByRegisteredClientIdAndPrincipalName(
            "client-delete", "user-delete");

    // Assert
    Assertions.assertEquals(1, deleted);

    OauthAuthorizationConsent found =
        oauthAuthorizationConsentMapper.selectByRegisteredClientIdAndPrincipalName(
            "client-delete", "user-delete");
    Assertions.assertNull(found);
  }
}
