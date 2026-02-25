package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent;
import jakarta.annotation.Resource;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/// OAuth 授权同意数据访问层测试类
///
/// 测试 OauthAuthorizationConsentRepository 的基本 CRUD 操作
///
/// @author lishangbu
/// @since 2025/8/20
class OauthAuthorizationConsentRepositoryTest extends AbstractRepositoryTest {

    @Resource private OauthAuthorizationConsentRepository oauthAuthorizationConsentRepository;

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
        OauthAuthorizationConsent inserted = oauthAuthorizationConsentRepository.save(consent);

        // Assert
        Assertions.assertNotNull(inserted);

        Optional<OauthAuthorizationConsent> foundOptional =
                oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                        "client-1", "user-1");
        Assertions.assertTrue(foundOptional.isPresent());
        OauthAuthorizationConsent oauthAuthorizationConsent = foundOptional.get();
        Assertions.assertEquals("client-1", oauthAuthorizationConsent.getRegisteredClientId());
        Assertions.assertEquals("user-1", oauthAuthorizationConsent.getPrincipalName());
        Assertions.assertEquals("scope1,scope2", oauthAuthorizationConsent.getAuthorities());
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
        oauthAuthorizationConsentRepository.save(consent);

        // 更新记录
        OauthAuthorizationConsent toUpdate = new OauthAuthorizationConsent();
        toUpdate.setRegisteredClientId("client-update");
        toUpdate.setPrincipalName("user-update");
        toUpdate.setAuthorities("c,d,e");

        // Act
        oauthAuthorizationConsentRepository.save(toUpdate);

        Optional<OauthAuthorizationConsent> foundOptional =
                oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                        "client-update", "user-update");
        Assertions.assertTrue(foundOptional.isPresent());
        Assertions.assertEquals("c,d,e", foundOptional.get().getAuthorities());
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
        oauthAuthorizationConsentRepository.save(consent);

        // Act
        oauthAuthorizationConsentRepository.deleteByRegisteredClientIdAndPrincipalName(
                "client-delete", "user-delete");

        // Assert
        Optional<OauthAuthorizationConsent> foundOptional =
                oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                        "client-delete", "user-delete");
        Assertions.assertTrue(foundOptional.isEmpty());
    }
}
