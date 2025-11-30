package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * Oauth2注册客户端(oauth_registered_client)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/9/14
 */
@Repository
public interface OauthRegisteredClientRepository
    extends ListCrudRepository<OauthRegisteredClient, String>,
        ListPagingAndSortingRepository<OauthRegisteredClient, String> {

  Optional<OauthRegisteredClient> findByClientId(String clientId);
}
