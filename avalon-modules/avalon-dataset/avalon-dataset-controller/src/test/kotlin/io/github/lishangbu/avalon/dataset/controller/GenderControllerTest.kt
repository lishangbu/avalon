package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.service.GenderService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class GenderControllerTest {
    @Test
    fun listGenders_delegatesToService() {
        val service = FakeGenderService()
        val controller = GenderController(service)
        val list = listOf(Gender {})
        service.listResult = list

        val result = controller.listGenders(1L, "female", "雌性")

        assertSame(list, result)
        assertEquals(1L, service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeGenderService()
        val controller = GenderController(service)
        val gender = Gender {}
        service.saveResult = gender

        val result = controller.save(gender)

        assertSame(gender, result)
        assertSame(gender, service.saved)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeGenderService()
        val controller = GenderController(service)
        val gender = Gender {}
        service.updateResult = gender

        val result = controller.update(gender)

        assertSame(gender, result)
        assertSame(gender, service.updated)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeGenderService()
        val controller = GenderController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeGenderService : GenderService {
        var listCondition: Gender? = null
        var saved: Gender? = null
        var updated: Gender? = null
        var removedId: Long? = null

        var listResult: List<Gender> = emptyList()
        var saveResult: Gender = Gender {}
        var updateResult: Gender = Gender {}

        override fun save(gender: Gender): Gender {
            saved = gender
            return saveResult
        }

        override fun update(gender: Gender): Gender {
            updated = gender
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(gender: Gender): List<Gender> {
            listCondition = gender
            return listResult
        }
    }
}
