package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.OAuthRefreshTokenReplay
import org.babyfish.jimmer.spring.repository.KRepository

/** 已使用 refresh token 摘要的 Jimmer 持久化边界。 */
interface OAuthRefreshTokenReplayRepository : KRepository<OAuthRefreshTokenReplay, String>
