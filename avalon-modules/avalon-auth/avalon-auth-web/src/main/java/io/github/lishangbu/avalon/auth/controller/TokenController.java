package io.github.lishangbu.avalon.auth.controller;

import io.github.lishangbu.avalon.auth.model.SignInPayload;
import io.github.lishangbu.avalon.auth.model.TokenInfo;
import io.github.lishangbu.avalon.security.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 令牌控制器
 *
 * @author lishangbu
 * @since 2025/4/9
 */
@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
public class TokenController {
  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtUtils;

  @PostMapping("/sign-in")
  public TokenInfo signIn(@RequestBody @Valid SignInPayload signInPayload) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                signInPayload.getUsername(), signInPayload.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    return new TokenInfo(
        jwtUtils.generateAccessTokenByAuthentication(authentication),
        jwtUtils.generateRefreshTokenByAuthentication());
  }
}
