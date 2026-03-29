package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * OAuth 授权同意仓储测试
 *
 * 验证授权同意记录的新增、更新和删除行为
 */
class OauthAuthorizationConsentRepositoryTest : AbstractRepositoryTest() {
    private companion object {
        const val EXISTING_REGISTERED_CLIENT_ID = "1"
    }

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
                    registeredClientId = EXISTING_REGISTERED_CLIENT_ID
                    principalName = "user-1"
                }
                authorities = "scope1,scope2"
            }

        val inserted = oauthAuthorizationConsentRepository.save(consent)

        assertNotNull(inserted)
        val foundOptional =
            oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                EXISTING_REGISTERED_CLIENT_ID,
                "user-1",
            )
        val oauthAuthorizationConsent = requireNotNull(foundOptional)
        assertEquals(EXISTING_REGISTERED_CLIENT_ID, oauthAuthorizationConsent.id.registeredClientId)
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
                    registeredClientId = EXISTING_REGISTERED_CLIENT_ID
                    principalName = "user-update"
                }
                authorities = "scope1,scope2"
            }
        oauthAuthorizationConsentRepository.save(consent)

        val toUpdate =
            OauthAuthorizationConsent {
                id {
                    registeredClientId = EXISTING_REGISTERED_CLIENT_ID
                    principalName = "user-update"
                }
                authorities = "c,d,e"
            }

        oauthAuthorizationConsentRepository.save(toUpdate)

        val foundOptional =
            oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                EXISTING_REGISTERED_CLIENT_ID,
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
                    registeredClientId = EXISTING_REGISTERED_CLIENT_ID
                    principalName = "user-delete"
                }
                authorities = "scope1,scope2"
            }
        oauthAuthorizationConsentRepository.save(consent)

        oauthAuthorizationConsentRepository.deleteByRegisteredClientIdAndPrincipalName(
            EXISTING_REGISTERED_CLIENT_ID,
            "user-delete",
        )

        val foundOptional =
            oauthAuthorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(
                EXISTING_REGISTERED_CLIENT_ID,
                "user-delete",
            )
        assertNull(foundOptional)
    }
}
