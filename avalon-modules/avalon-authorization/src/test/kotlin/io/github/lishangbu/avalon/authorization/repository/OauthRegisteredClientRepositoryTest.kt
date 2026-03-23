package io.github.lishangbu.avalon.authorization.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * OAuth 注册客户端 Repository 测试
 *
 * 验证根据 clientId 查询客户端配置的能力，依赖数据库初始化的数据
 *
 * @author lishangbu
 * @since 2025/8/20
 */
class OauthRegisteredClientRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var oauth2RegisteredClientRepository: Oauth2RegisteredClientRepository

    @Test
    fun testFindByClientId() {
        val oauthRegisteredClient =
            requireNotNull(oauth2RegisteredClientRepository.findByClientId("client"))
        assertEquals("1", oauthRegisteredClient.id)
        assertEquals("client", oauthRegisteredClient.clientId)
        assertEquals("{noop}client", oauthRegisteredClient.clientSecret)
    }
}
