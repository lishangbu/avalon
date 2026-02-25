package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/// 用户授权确认表(oauth_authorization_consent)数据库访问层
///
/// 提供基础的 CRUD 操作
///
/// @author lishangbu
/// @since 2025/9/14
@Repository
public interface OauthAuthorizationConsentRepository
        extends JpaRepository<
                        OauthAuthorizationConsent,
                        OauthAuthorizationConsent.AuthorizationConsentId>,
                JpaSpecificationExecutor<OauthAuthorizationConsent> {
    Optional<OauthAuthorizationConsent> findByRegisteredClientIdAndPrincipalName(
            String registeredClientId, String principalName);

    void deleteByRegisteredClientIdAndPrincipalName(
            String registeredClientId, String principalName);
}
