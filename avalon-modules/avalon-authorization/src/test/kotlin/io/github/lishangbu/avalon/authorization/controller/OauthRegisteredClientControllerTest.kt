package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.service.OauthRegisteredClientService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class OauthRegisteredClientControllerTest {
    @Test
    fun getPage_delegatesToService() {
        val service = FakeOauthRegisteredClientService()
        val controller = OauthRegisteredClientController(service)
        val pageable = PageRequest.of(0, 10)
        val page = Page(listOf(OauthRegisteredClient { id = "1" }), 1, 1)
        service.pageResult = page

        val result =
            controller.getPage(
                pageable = pageable,
                id = "1",
                clientId = "client",
                clientName = "Test Client",
            )

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
        assertEquals("client", service.pageCondition!!.clientId)
        assertEquals("Test Client", service.pageCondition!!.clientName)
    }

    @Test
    fun list_delegatesToService() {
        val service = FakeOauthRegisteredClientService()
        val controller = OauthRegisteredClientController(service)
        val client = OauthRegisteredClient { id = "1" }
        service.listResult = listOf(client)

        val result =
            controller.list(
                id = "1",
                clientId = "client",
                clientName = "Test Client",
            )

        assertSame(service.listResult, result)
        assertEquals("1", service.listCondition!!.id)
        assertEquals("client", service.listCondition!!.clientId)
        assertEquals("Test Client", service.listCondition!!.clientName)
    }

    private class FakeOauthRegisteredClientService : OauthRegisteredClientService {
        var pageCondition: OauthRegisteredClient? = null
        var listCondition: OauthRegisteredClient? = null
        var pageable: Pageable? = null
        var pageResult: Page<OauthRegisteredClient> = Page(emptyList(), 0, 0)
        var listResult: List<OauthRegisteredClient> = emptyList()

        override fun getPageByCondition(
            registeredClient: OauthRegisteredClient,
            pageable: Pageable,
        ): Page<OauthRegisteredClient> {
            pageCondition = registeredClient
            this.pageable = pageable
            return pageResult
        }

        override fun listByCondition(registeredClient: OauthRegisteredClient): List<OauthRegisteredClient> {
            listCondition = registeredClient
            return listResult
        }

        override fun getById(id: String): OauthRegisteredClient? = error("Not used")

        override fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient = error("Not used")

        override fun update(registeredClient: OauthRegisteredClient): OauthRegisteredClient = error("Not used")

        override fun removeById(id: String) = Unit
    }
}
