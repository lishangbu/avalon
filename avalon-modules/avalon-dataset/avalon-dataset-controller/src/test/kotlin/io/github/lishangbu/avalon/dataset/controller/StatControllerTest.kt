package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.service.StatService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class StatControllerTest {
    @Test
    fun getStatPage_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<Stat> = Page(listOf(Stat()), 1, 1)
        service.pageResult = page

        val result = controller.getStatPage(pageable, 1L, "hp", "生命", 1, false, 2L)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals(1L, service.pageCondition!!.id)
    }

    @Test
    fun listStats_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)
        val list = listOf(Stat())
        service.listResult = list

        val result = controller.listStats(1L, "hp", "生命", 1, false, 2L)

        assertSame(list, result)
        assertEquals(1L, service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)
        val stat = Stat()
        service.saveResult = stat

        val result = controller.save(stat)

        assertSame(stat, result)
        assertSame(stat, service.saved)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)
        val stat = Stat()
        service.updateResult = stat

        val result = controller.update(stat)

        assertSame(stat, result)
        assertSame(stat, service.updated)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeStatService : StatService {
        var pageCondition: Stat? = null
        var pageable: Pageable? = null
        var listCondition: Stat? = null
        var saved: Stat? = null
        var updated: Stat? = null
        var removedId: Long? = null

        var pageResult: Page<Stat> = Page(emptyList(), 0, 0)
        var listResult: List<Stat> = emptyList()
        var saveResult: Stat = Stat()
        var updateResult: Stat = Stat()

        override fun getPageByCondition(
            stat: Stat,
            pageable: Pageable,
        ): Page<Stat> {
            pageCondition = stat
            this.pageable = pageable
            return pageResult
        }

        override fun save(stat: Stat): Stat {
            saved = stat
            return saveResult
        }

        override fun update(stat: Stat): Stat {
            updated = stat
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(stat: Stat): List<Stat> {
            listCondition = stat
            return listResult
        }
    }
}
