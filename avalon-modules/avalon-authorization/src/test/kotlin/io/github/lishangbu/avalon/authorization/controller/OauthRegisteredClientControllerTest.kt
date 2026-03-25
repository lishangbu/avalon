package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
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
                specification =
                    OauthRegisteredClientSpecification(
                        id = "1",
                        clientId = "client",
                        clientName = "Test Client",
                    ),
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
                OauthRegisteredClientSpecification(
                    id = "1",
                    clientId = "client",
                    clientName = "Test Client",
                ),
            )

        assertSame(service.listResult, result)
        assertEquals("1", service.listCondition!!.id)
        assertEquals("client", service.listCondition!!.clientId)
        assertEquals("Test Client", service.listCondition!!.clientName)
    }

    @Test
    fun getById_delegatesToService() {
        val service = FakeOauthRegisteredClientService()
        val controller = OauthRegisteredClientController(service)
        val client = OauthRegisteredClient { id = "client-1" }
        service.getByIdResult = client

        val result = controller.getById("client-1")

        assertSame(client, result)
        assertEquals("client-1", service.getByIdArgument)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeOauthRegisteredClientService()
        val controller = OauthRegisteredClientController(service)
        val request = OauthRegisteredClient { id = "client-1" }
        val saved = OauthRegisteredClient { id = "saved-client" }
        service.saveResult = saved

        val result = controller.save(request)

        assertSame(saved, result)
        assertSame(request, service.savedClient)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeOauthRegisteredClientService()
        val controller = OauthRegisteredClientController(service)
        val request = OauthRegisteredClient { id = "client-1" }
        val updated = OauthRegisteredClient { id = "updated-client" }
        service.updateResult = updated

        val result = controller.update(request)

        assertSame(updated, result)
        assertSame(request, service.updatedClient)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeOauthRegisteredClientService()
        val controller = OauthRegisteredClientController(service)

        controller.deleteById("client-1")

        assertEquals("client-1", service.removedId)
    }

    private class FakeOauthRegisteredClientService : OauthRegisteredClientService {
        var pageCondition: OauthRegisteredClientSpecification? = null
        var listCondition: OauthRegisteredClientSpecification? = null
        var pageable: Pageable? = null
        var pageResult: Page<OauthRegisteredClient> = Page(emptyList(), 0, 0)
        var listResult: List<OauthRegisteredClient> = emptyList()
        var getByIdArgument: String? = null
        var getByIdResult: OauthRegisteredClient? = null
        var savedClient: OauthRegisteredClient? = null
        var saveResult: OauthRegisteredClient? = null
        var updatedClient: OauthRegisteredClient? = null
        var updateResult: OauthRegisteredClient? = null
        var removedId: String? = null

        override fun getPageByCondition(
            specification: OauthRegisteredClientSpecification,
            pageable: Pageable,
        ): Page<OauthRegisteredClient> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun listByCondition(specification: OauthRegisteredClientSpecification): List<OauthRegisteredClient> {
            listCondition = specification
            return listResult
        }

        override fun getById(id: String): OauthRegisteredClient? {
            getByIdArgument = id
            return getByIdResult
        }

        override fun save(registeredClient: OauthRegisteredClient): OauthRegisteredClient {
            savedClient = registeredClient
            return requireNotNull(saveResult)
        }

        override fun update(registeredClient: OauthRegisteredClient): OauthRegisteredClient {
            updatedClient = registeredClient
            return requireNotNull(updateResult)
        }

        override fun removeById(id: String) {
            removedId = id
        }
    }
}
