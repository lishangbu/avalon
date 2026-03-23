package io.github.lishangbu.avalon.authorization.service.impl

import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Service

/**
 * 客户端注册信息仓储适配器
 *
 * 提供 [ClientRegistrationRepository] 的空实现
 *
 * @author lishangbu
 * @since 2025/8/25
 */
@Service
class DefaultClientRegistrationRepository : ClientRegistrationRepository {
    /** 当前未提供客户端注册信息 */
    override fun findByRegistrationId(registrationId: String): ClientRegistration? = null
}
