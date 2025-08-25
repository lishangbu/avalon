package io.github.lishangbu.avalon.authorization.service;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

/**
 * A repository for OAuth 2.0 / OpenID Connect 1.0 {@link ClientRegistration}(s).
 *
 * <p><b>NOTE:</b> Client registration information is ultimately stored and owned by the associated
 * Authorization Server. Therefore, this repository provides the capability to store a sub-set copy
 * of the <i>primary</i> client registration information externally from the Authorization Server.
 *
 * @author lishangbu
 * @since 2025/8/25
 */
@Service
public class DefaultClientRegistrationRepository implements ClientRegistrationRepository {
  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    return null;
  }
}
