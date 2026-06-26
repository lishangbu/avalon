package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.OAuth2Client
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * OAuth 客户端注册信息的 Jimmer Repository。
 */
interface OAuth2ClientRepository : KRepository<OAuth2Client, Long>
