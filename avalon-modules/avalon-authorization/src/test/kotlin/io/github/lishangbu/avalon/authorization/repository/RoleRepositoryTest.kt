package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.authorization.entity.dto.RoleSpecification
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.data.domain.PageRequest
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional

/**
 * 角色信息实体的 Repository 测试
 *
 * 验证角色查询、插入、更新与删除流程，依赖数据库初始化的测试数据
 *
 * @author lishangbu
 * @since 2025/8/25
 */
@Transactional(rollbackFor = [Exception::class])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RoleRepositoryTest : AbstractRepositoryTest() {
    companion object {
        private var insertId: Long? = null
    }

    @Resource
    private lateinit var roleRepository: RoleRepository

    @Test
    @Order(1)
    fun testSelectRoleById() {
        val role = requireNotNull(roleRepository.findNullable(1L))
        assertEquals("ROLE_SUPER_ADMIN", role.code)
        assertEquals("超级管理员", role.name)
        assertEquals(1L, role.id)
        assertTrue(role.enabled == true)
        assertNull(role.readOrNull { menus })
    }

    @Test
    @Order(2)
    fun testSelectRoleByIdWithMenus() {
        val role = requireNotNull(roleRepository.findNullable(1L, AuthorizationFetchers.ROLE_WITH_MENUS))
        assertFalse(role.menus.isEmpty())
    }

    @Test
    @Order(3)
    fun testFindAllBySpecification() {
        val roles = roleRepository.findAll(RoleSpecification(code = "ROLE_SUPER_ADMIN"))
        assertEquals(1, roles.size)
        assertEquals("ROLE_SUPER_ADMIN", roles.first().code)
        assertNull(roles.first().readOrNull { menus })
    }

    @Test
    @Order(4)
    fun testListWithMenusAndPageQueries() {
        val roles = roleRepository.listWithMenus(RoleSpecification(id = "1"))
        assertEquals(1, roles.size)
        assertFalse(roles.first().menus.isEmpty())

        val page = roleRepository.findAll(RoleSpecification(enabled = true), PageRequest.of(0, 10))
        assertFalse(page.rows.isEmpty())
        assertTrue(page.rows.all { it.enabled == true })

        val pageWithMenus = roleRepository.pageWithMenus(RoleSpecification(id = "1"), PageRequest.of(0, 10))
        assertEquals(1, pageWithMenus.totalRowCount)
        assertFalse(
            pageWithMenus
                .rows
                .first()
                .menus
                .isEmpty(),
        )
    }

    @Test
    @Order(5)
    @Commit
    fun testInsertRole() {
        val role =
            roleRepository.save(
                Role {
                    code = "unit_test"
                    name = "为单元测试而生"
                    enabled = true
                },
                SaveMode.INSERT_ONLY,
            )
        assertNotNull(role.id)
        insertId = role.id
    }

    @Test
    @Order(6)
    @Commit
    fun testUpdateRoleById() {
        val role = requireNotNull(roleRepository.findNullable(insertId!!))
        roleRepository.save(
            Role(role) {
                name = "测试员1"
                enabled = false
                code = "ROLE_TEST1"
            },
        )
    }

    @Test
    @Order(7)
    fun testSelectUpdatedRoleById() {
        val role = requireNotNull(roleRepository.findNullable(insertId!!))
        assertEquals("ROLE_TEST1", role.code)
        assertEquals("测试员1", role.name)
        assertEquals(insertId, role.id)
        assertFalse(role.enabled == true)
    }

    @Test
    @Order(8)
    fun testDeleteById() {
        roleRepository.deleteById(insertId!!)
    }
}
