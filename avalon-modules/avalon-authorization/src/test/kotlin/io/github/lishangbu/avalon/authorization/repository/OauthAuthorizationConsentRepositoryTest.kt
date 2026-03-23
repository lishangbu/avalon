package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * OAuth 授权同意数据访问层测试类
 *
 * 测试 OauthAuthorizationConsentRepository 的基本 CRUD 操作
 *
 * @author lishangbu
 * @since 2025/8/20
 */
class OauthAuthorizationConsentRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var oauthAuthorizationConsentRepository: OauthAuthorizationConsentRepository

    /**
     * 测试插入和查询 OAuth 授权同意记录
     *
     * 验证插入操作成功后能够通过客户端ID和主体名称查询到记录
     */
    @Test
    fun shouldInsertAndFindConsentByClientIdAndPrincipalName() {
        val consent =
            OauthAuthorizationConsent {
                id {
                    registeredClientId = "client-1"
                    principalName = "user-1"
                }
                authorities = "scope1,scope2"
            }

        val inserted = oauthAuthorizationConsentRepository.save(consent)

        assertNotNull(inserted)
        val foundOptional =
            oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                "client-1",
                "user-1",
            )
        val oauthAuthorizationConsent = requireNotNull(foundOptional)
        assertEquals("client-1", oauthAuthorizationConsent.id.registeredClientId)
        assertEquals("user-1", oauthAuthorizationConsent.id.principalName)
        assertEquals("scope1,scope2", oauthAuthorizationConsent.authorities)
    }

    /**
     * 测试通过 ID 更新 OAuth 授权同意记录
     *
     * 验证更新操作成功后能够通过客户端ID和主体名称查询到最新的记录
     */
    @Test
    fun shouldUpdateConsentById() {
        val consent =
            OauthAuthorizationConsent {
                id {
                    registeredClientId = "client-update"
                    principalName = "user-update"
                }
                authorities = "scope1,scope2"
            }
        oauthAuthorizationConsentRepository.save(consent)

        val toUpdate =
            OauthAuthorizationConsent {
                id {
                    registeredClientId = "client-update"
                    principalName = "user-update"
                }
                authorities = "c,d,e"
            }

        oauthAuthorizationConsentRepository.save(toUpdate)

        val foundOptional =
            oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                "client-update",
                "user-update",
            )
        val updatedConsent = requireNotNull(foundOptional)
        assertEquals("c,d,e", updatedConsent.authorities)
    }

    /**
     * 测试通过客户端ID和主体名称删除 OAuth 授权同意记录
     *
     * 验证删除操作成功后无法再通过客户端ID和主体名称查询到记录
     */
    @Test
    fun shouldDeleteConsentByClientIdAndPrincipalName() {
        val consent =
            OauthAuthorizationConsent {
                id {
                    registeredClientId = "client-delete"
                    principalName = "user-delete"
                }
                authorities = "scope1,scope2"
            }
        oauthAuthorizationConsentRepository.save(consent)

        oauthAuthorizationConsentRepository.deleteByRegisteredClientIdAndPrincipalName(
            "client-delete",
            "user-delete",
        )

        val foundOptional =
            oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                "client-delete",
                "user-delete",
            )
        assertNull(foundOptional)
    }
}
