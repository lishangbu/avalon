package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.PageRequest

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

    @Test
    fun testFindPage() {
        val page = oauth2RegisteredClientRepository.findAll(null, PageRequest.of(0, 10))
        assertEquals(2, page.totalRowCount)
        assertFalse(page.rows.isEmpty())
        assertEquals("1", page.rows.first().id)
    }

    @Test
    fun testFindPageWithEmptyExample() {
        val page =
            oauth2RegisteredClientRepository.findAll(
                Example.of(
                    OauthRegisteredClient {},
                    ExampleMatcher
                        .matching()
                        .withIgnoreNullValues()
                        .withMatcher("clientId", ExampleMatcher.GenericPropertyMatchers.contains())
                        .withMatcher("clientName", ExampleMatcher.GenericPropertyMatchers.contains()),
                ),
                PageRequest.of(0, 10),
            )
        assertEquals(2, page.totalRowCount)
        assertFalse(page.rows.isEmpty())
    }
}
