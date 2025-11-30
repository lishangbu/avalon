package io.github.lishangbu.avalon.authorization.service.impl;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

/**
 * 默认Oauth2授权服务
 *
 * @author lishangbu
 * @since 2025/11/30
 */
@Service
public class DefaultOAuth2AuthorizationService extends JdbcOAuth2AuthorizationService {

  public DefaultOAuth2AuthorizationService(JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {
    super(jdbcOperations, registeredClientRepository, new DefaultLobHandler());
  }
}
