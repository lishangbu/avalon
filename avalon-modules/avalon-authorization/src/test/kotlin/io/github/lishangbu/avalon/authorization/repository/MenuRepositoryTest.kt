package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Menu
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional

/**
 * 菜单 Repository 测试
 *
 * 验证菜单查询、插入、更新与删除流程，依赖数据库初始化的测试数据
 *
 * @author lishangbu
 * @since 2025/12/6
 */
@Transactional(rollbackFor = [Exception::class])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MenuRepositoryTest : AbstractRepositoryTest() {
    companion object {
        private var insertId: Long? = null
    }

    @Resource
    private lateinit var menuRepository: MenuRepository

    @Test
    @Order(1)
    fun testSelectMenuById() {
        val menu = requireNotNull(menuRepository.findNullable(1L, AuthorizationFetchers.MENU))
        assertEquals("dashboard", menu.key)
        assertEquals("仪表板", menu.label)
        assertEquals(1L, menu.id)
    }

    @Test
    @Order(2)
    @Commit
    fun testInsertMenu() {
        val menu =
            menuRepository.save(
                Menu {
                    key = "unit_test_menu"
                    label = "单元测试菜单"
                    path = "/unit-test"
                    sortingOrder = 100
                    disabled = false
                    show = true
                },
                SaveMode.INSERT_ONLY,
            )
        assertNotNull(menu.id)
        insertId = menu.id
    }

    @Test
    @Order(3)
    @Commit
    fun testUpdateMenuById() {
        val menu = requireNotNull(menuRepository.findNullable(insertId!!, AuthorizationFetchers.MENU))
        menuRepository.save(
            Menu(menu) {
                label = "更新单元测试菜单"
                disabled = true
            },
        )
    }

    @Test
    @Order(4)
    fun testSelectUpdatedMenuById() {
        val menu = requireNotNull(menuRepository.findNullable(insertId!!, AuthorizationFetchers.MENU))
        assertEquals("更新单元测试菜单", menu.label)
        assertTrue(menu.disabled == true)
    }

    @Test
    @Order(5)
    fun testFindAllByRoleCodes() {
        val menus = menuRepository.listByRoleCodes(listOf("ROLE_SUPER_ADMIN"))
        assertNotNull(menus)
        assertFalse(menus.isEmpty())
        menus.zipWithNext().forEach { (previous, current) ->
            val previousOrder = previous.sortingOrder ?: Int.MAX_VALUE
            val currentOrder = current.sortingOrder ?: Int.MAX_VALUE
            assertTrue(
                previousOrder < currentOrder || (previousOrder == currentOrder && previous.id <= current.id),
            )
        }
    }

    @Test
    @Order(6)
    fun testDeleteById() {
        menuRepository.deleteById(insertId!!)
    }
}
