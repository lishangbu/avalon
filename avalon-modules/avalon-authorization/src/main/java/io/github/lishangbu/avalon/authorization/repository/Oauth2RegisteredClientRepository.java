package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/// OauthRegisteredClient 数据访问 Mapper
///
/// 提供对 OauthRegisteredClient 实体的查询方法
///
/// @author lishangbu
/// @since  2023-10-08
@Repository
public interface Oauth2RegisteredClientRepository
    extends JpaRepository<OauthRegisteredClient, String>,
        JpaSpecificationExecutor<OauthRegisteredClient> {

  Optional<OauthRegisteredClient> findByClientId(String clientId);
}
