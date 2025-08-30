package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.mapper.Oauth2AuthorizationConsentMapper;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementations of this interface are responsible for the management of {@link
 * OAuth2AuthorizationConsent OAuth 2.0 Authorization Consent(s)}.
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Service
@RequiredArgsConstructor
public class DefaultOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {
  private final Oauth2AuthorizationConsentMapper oauth2AuthorizationConsentMapper;
  private final RegisteredClientRepository registeredClientRepository;

  @Override
  public void save(OAuth2AuthorizationConsent authorizationConsent) {
    Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
    OAuth2AuthorizationConsent existingAuthorizationConsent =
        findById(
            authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
    if (existingAuthorizationConsent == null) {
      this.oauth2AuthorizationConsentMapper.insert(toEntity(authorizationConsent));
    } else {
      this.oauth2AuthorizationConsentMapper.updateByByRegisteredClientIdAndPrincipalName(
          toEntity(authorizationConsent));
    }
  }

  @Override
  public void remove(OAuth2AuthorizationConsent authorizationConsent) {
    Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
    this.oauth2AuthorizationConsentMapper.deleteByRegisteredClientIdAndPrincipalName(
        authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
  }

  @Override
  public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
    Assert.hasText(registeredClientId, "registeredClientId cannot be empty");
    Assert.hasText(principalName, "principalName cannot be empty");
    return this.oauth2AuthorizationConsentMapper
        .selectByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
        .map(this::toObject)
        .orElse(null);
  }

  private OAuth2AuthorizationConsent toObject(
      io.github.lishangbu.avalon.authorization.entity.Oauth2AuthorizationConsent
          authorizationConsent) {
    String registeredClientId = authorizationConsent.getRegisteredClientId();
    RegisteredClient registeredClient =
        this.registeredClientRepository.findById(registeredClientId);
    if (registeredClient == null) {
      throw new IllegalStateException(
          "The RegisteredClient with id '"
              + registeredClientId
              + "' was not found in the RegisteredClientRepository.");
    }

    OAuth2AuthorizationConsent.Builder builder =
        OAuth2AuthorizationConsent.withId(
            registeredClientId, authorizationConsent.getPrincipalName());
    if (authorizationConsent.getAuthorities() != null) {
      for (String authority :
          StringUtils.commaDelimitedListToSet(authorizationConsent.getAuthorities())) {
        builder.authority(new SimpleGrantedAuthority(authority));
      }
    }

    return builder.build();
  }

  private io.github.lishangbu.avalon.authorization.entity.Oauth2AuthorizationConsent toEntity(
      OAuth2AuthorizationConsent authorizationConsent) {

    io.github.lishangbu.avalon.authorization.entity.Oauth2AuthorizationConsent entity =
        new io.github.lishangbu.avalon.authorization.entity.Oauth2AuthorizationConsent();
    entity.setRegisteredClientId(authorizationConsent.getRegisteredClientId());
    entity.setPrincipalName(authorizationConsent.getPrincipalName());

    Set<String> authorities = new HashSet<>();
    for (GrantedAuthority authority : authorizationConsent.getAuthorities()) {
      authorities.add(authority.getAuthority());
    }
    entity.setAuthorities(StringUtils.collectionToCommaDelimitedString(authorities));

    return entity;
  }
}
