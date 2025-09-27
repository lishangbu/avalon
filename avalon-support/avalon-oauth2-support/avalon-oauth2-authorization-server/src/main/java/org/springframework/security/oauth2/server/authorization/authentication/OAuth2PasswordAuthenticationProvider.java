package org.springframework.security.oauth2.server.authorization.authentication;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.core.OAuth2PasswordErrorCodes;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.security.Principal;
import java.util.*;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.Assert;

/**
 * @author xuxiaowei
 * @author lishangbu
 * @see OAuth2AuthorizationCodeAuthenticationProvider
 * @see OAuth2RefreshTokenAuthenticationProvider
 * @see OAuth2ClientCredentialsAuthenticationProvider
 * @see UserInfo
 * @since 2025/9/28
 */
public final class OAuth2PasswordAuthenticationProvider implements AuthenticationProvider {

  private static final String ERROR_URI =
      "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

  private static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE =
      new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

  private final Log logger = LogFactory.getLog(getClass());

  private final OAuth2AuthorizationService authorizationService;

  private final UserDetailsService userDetailsService;

  private final PasswordEncoder passwordEncoder;

  private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

  private Consumer<OAuth2PasswordAuthenticationContext> authenticationValidator =
      new OAuth2PasswordAuthenticationValidator();

  public OAuth2PasswordAuthenticationProvider(
      HttpSecurity httpSecurity,
      OAuth2AuthorizationServerConfigurer authorizationServerConfigurer,
      OAuth2AuthorizationService authorizationService,
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
      Consumer<List<AuthenticationConverter>> accessTokenRequestConvertersConsumer) {
    Assert.notNull(httpSecurity, "httpSecurity cannot be null");
    Assert.notNull(authorizationServerConfigurer, "authorizationServerConfigurer cannot be null");
    Assert.notNull(authorizationService, "authorizationService cannot be null");
    Assert.notNull(userDetailsService, "userDetailsService cannot be null");
    Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
    Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
    Assert.notNull(
        accessTokenRequestConvertersConsumer,
        "accessTokenRequestConvertersConsumer cannot be null");
    this.authorizationService = authorizationService;
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.tokenGenerator = tokenGenerator;
    authorizationServerConfigurer.tokenEndpoint(
        tokenEndpointCustomizer ->
            tokenEndpointCustomizer.accessTokenRequestConverters(
                accessTokenRequestConvertersConsumer));
    httpSecurity.authenticationProvider(this);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    OAuth2PasswordAuthorizationGrantAuthenticationToken passwordGrantAuthenticationToken =
        (OAuth2PasswordAuthorizationGrantAuthenticationToken) authentication;

    OAuth2ClientAuthenticationToken clientAuthenticationToken =
        (OAuth2ClientAuthenticationToken) passwordGrantAuthenticationToken.getPrincipal();

    RegisteredClient registeredClient = clientAuthenticationToken.getRegisteredClient();

    if (this.logger.isTraceEnabled()) {
      this.logger.trace("Retrieved registered client");
    }

    if (!registeredClient
        .getAuthorizationGrantTypes()
        .contains(AuthorizationGrantTypeSupport.PASSWORD)) {
      if (this.logger.isDebugEnabled()) {
        this.logger.debug(
            LogMessage.format(
                "Invalid request: requested grant_type is not allowed"
                    + " for registered client '%s'",
                registeredClient.getId()));
      }
      throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
    }

    OAuth2PasswordAuthenticationContext authenticationContext =
        OAuth2PasswordAuthenticationContext.with(passwordGrantAuthenticationToken)
            .registeredClient(registeredClient)
            .build();
    this.authenticationValidator.accept(authenticationContext);

    Set<String> authorizedScopes =
        new LinkedHashSet<>(passwordGrantAuthenticationToken.getScopes());

    Object credentials = passwordGrantAuthenticationToken.getCredentials();
    String username = passwordGrantAuthenticationToken.getUsername();
    String password = passwordGrantAuthenticationToken.getPassword();
    Map<String, Object> additionalParameters =
        new HashMap<>(passwordGrantAuthenticationToken.getAdditionalParameters());

    UserDetails userDetails;
    try {
      userDetails = userDetailsService.loadUserByUsername(username);
    } catch (UsernameNotFoundException e) {
      OAuth2Error error =
          new OAuth2Error(
              OAuth2PasswordErrorCodes.INVALID_USERNAME,
              "Invalid username: username does not exist",
              ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    boolean matches = passwordEncoder.matches(password, userDetails.getPassword());
    if (!matches) {
      OAuth2Error error =
          new OAuth2Error(
              OAuth2PasswordErrorCodes.INVALID_PASSWORD,
              "Invalid password: password does not match",
              ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    boolean enabled = userDetails.isEnabled();
    if (!enabled) {
      OAuth2Error error =
          new OAuth2Error(
              OAuth2PasswordErrorCodes.USER_DISABLED, "User account is disabled", ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    boolean accountNonExpired = userDetails.isAccountNonExpired();
    if (!accountNonExpired) {
      OAuth2Error error =
          new OAuth2Error(
              OAuth2PasswordErrorCodes.ACCOUNT_EXPIRED, "User account has expired", ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    boolean credentialsNonExpired = userDetails.isCredentialsNonExpired();
    if (!credentialsNonExpired) {
      OAuth2Error error =
          new OAuth2Error(
              OAuth2PasswordErrorCodes.CREDENTIALS_EXPIRED,
              "User credentials have expired",
              ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    boolean accountNonLocked = userDetails.isAccountNonLocked();
    if (!accountNonLocked) {
      OAuth2Error error =
          new OAuth2Error(
              OAuth2PasswordErrorCodes.ACCOUNT_LOCKED, "User account is locked", ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

    OAuth2PasswordAuthenticationToken passwordAuthenticationToken =
        new OAuth2PasswordAuthenticationToken(
            username, registeredClient, authorities, additionalParameters);

    DefaultOAuth2TokenContext.Builder tokenContextBuilder =
        DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(passwordAuthenticationToken)
            .authorizationServerContext(AuthorizationServerContextHolder.getContext())
            .authorizedScopes(authorizedScopes)
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
            .authorizationGrant(passwordGrantAuthenticationToken);

    // ----- Access token -----
    OAuth2TokenContext tokenContext =
        tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();

    OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);

    if (generatedAccessToken == null) {
      OAuth2Error error =
          new OAuth2Error(
              OAuth2ErrorCodes.SERVER_ERROR,
              "The token generator failed to generate the access token.",
              ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    if (this.logger.isTraceEnabled()) {
      this.logger.trace("Generated access token");
    }

    OAuth2Authorization.Builder authorizationBuilder =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(username)
            .authorizationGrantType(AuthorizationGrantTypeSupport.PASSWORD)
            .authorizedScopes(authorizedScopes)
            .attribute(Principal.class.getName(), passwordAuthenticationToken);

    OAuth2AccessToken accessToken =
        OAuth2AuthenticationProviderUtils.accessToken(
            authorizationBuilder, generatedAccessToken, tokenContext);

    // ----- Refresh token -----
    OAuth2RefreshToken refreshToken = null;
    // Do not issue refresh token to public client
    if (registeredClient
        .getAuthorizationGrantTypes()
        .contains(AuthorizationGrantType.REFRESH_TOKEN)) {
      tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
      OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(tokenContext);
      if (generatedRefreshToken != null) {
        if (!(generatedRefreshToken instanceof OAuth2RefreshToken)) {
          OAuth2Error error =
              new OAuth2Error(
                  OAuth2ErrorCodes.SERVER_ERROR,
                  "The token generator failed to generate a valid refresh token.",
                  ERROR_URI);
          throw new OAuth2AuthenticationException(error);
        }

        if (this.logger.isTraceEnabled()) {
          this.logger.trace("Generated refresh token");
        }

        refreshToken = (OAuth2RefreshToken) generatedRefreshToken;
        authorizationBuilder.refreshToken(refreshToken);
      }
    }

    // ----- ID token -----
    OidcIdToken idToken;
    if (authorizedScopes.contains(OidcScopes.OPENID)) {
      // @formatter:off
      tokenContext =
          tokenContextBuilder
              .tokenType(ID_TOKEN_TOKEN_TYPE)
              // ID token customizer may need access to the access token and/or refresh token
              .authorization(authorizationBuilder.build())
              .build();
      // @formatter:on
      OAuth2Token generatedIdToken = this.tokenGenerator.generate(tokenContext);
      if (!(generatedIdToken instanceof Jwt)) {
        OAuth2Error error =
            new OAuth2Error(
                OAuth2ErrorCodes.SERVER_ERROR,
                "The token generator failed to generate the ID token.",
                ERROR_URI);
        throw new OAuth2AuthenticationException(error);
      }

      if (this.logger.isTraceEnabled()) {
        this.logger.trace("Generated id token");
      }

      idToken =
          new OidcIdToken(
              generatedIdToken.getTokenValue(),
              generatedIdToken.getIssuedAt(),
              generatedIdToken.getExpiresAt(),
              ((Jwt) generatedIdToken).getClaims());
      authorizationBuilder.token(
          idToken,
          (metadata) ->
              metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, idToken.getClaims()));
    } else {
      idToken = null;
    }
    OAuth2Authorization authorization = authorizationBuilder.build();

    this.authorizationService.save(authorization);

    if (this.logger.isTraceEnabled()) {
      this.logger.trace("Saved authorization");
    }

    if (idToken != null) {
      additionalParameters = new HashMap<>();
      additionalParameters.put(OidcParameterNames.ID_TOKEN, idToken.getTokenValue());
    }

    if (this.logger.isTraceEnabled()) {
      // This log is kept separate for consistency with other providers
      this.logger.trace("Authenticated token request");
    }

    if (userDetails instanceof UserInfo userDetailsInfo) {
      Map<String, Object> map = userDetailsInfo.getAdditionalParameters();
      if (map != null) {
        additionalParameters.putAll(map);
      }
    }

    OAuth2AccessTokenAuthenticationToken accessTokenAuthenticationResult =
        new OAuth2AccessTokenAuthenticationToken(
            registeredClient,
            passwordAuthenticationToken,
            accessToken,
            refreshToken,
            additionalParameters);
    accessTokenAuthenticationResult.setDetails(passwordGrantAuthenticationToken.getDetails());
    return accessTokenAuthenticationResult;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return OAuth2PasswordAuthorizationGrantAuthenticationToken.class.isAssignableFrom(
        authentication);
  }
}
