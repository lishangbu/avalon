package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Role
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
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
        val role = requireNotNull(roleRepository.findById(1L))
        assertEquals("ROLE_SUPER_ADMIN", role.code)
        assertEquals("超级管理员", role.name)
        assertEquals(1L, role.id)
        assertTrue(role.enabled == true)
        assertNull(role.readOrNull { menus })
    }

    @Test
    @Order(2)
    fun testSelectRoleByIdWithMenus() {
        val role = requireNotNull(roleRepository.findByIdWithMenus(1L))
        assertFalse(role.menus.isEmpty())
    }

    @Test
    @Order(3)
    @Commit
    fun testInsertRole() {
        val role =
            roleRepository.save(
                Role {
                    code = "unit_test"
                    name = "为单元测试而生"
                    enabled = true
                },
            )
        assertNotNull(role.id)
        insertId = role.id
    }

    @Test
    @Order(4)
    @Commit
    fun testUpdateRoleById() {
        val role = requireNotNull(roleRepository.findById(insertId!!))
        roleRepository.save(
            Role(role) {
                name = "测试员1"
                enabled = false
                code = "ROLE_TEST1"
            },
        )
    }

    @Test
    @Order(5)
    fun testSelectUpdatedRoleById() {
        val role = requireNotNull(roleRepository.findById(insertId!!))
        assertEquals("ROLE_TEST1", role.code)
        assertEquals("测试员1", role.name)
        assertEquals(insertId, role.id)
        assertFalse(role.enabled == true)
    }

    @Test
    @Order(6)
    fun testDeleteById() {
        roleRepository.deleteById(insertId!!)
    }
}
