package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户授权确认表(oauth_authorization_consent)数据库访问层
 *
 * @author lishangbu
 * @since 2025/9/14
 */
@Repository
public interface OauthAuthorizationConsentRepository
    extends ListCrudRepository<
            OauthAuthorizationConsent, OauthAuthorizationConsent.AuthorizationConsentId>,
        ListPagingAndSortingRepository<
            OauthAuthorizationConsent, OauthAuthorizationConsent.AuthorizationConsentId> {
  Optional<OauthAuthorizationConsent> findByRegisteredClientIdAndPrincipalName(
      String registeredClientId, String principalName);

  void deleteByRegisteredClientIdAndPrincipalName(String registeredClientId, String principalName);
}
