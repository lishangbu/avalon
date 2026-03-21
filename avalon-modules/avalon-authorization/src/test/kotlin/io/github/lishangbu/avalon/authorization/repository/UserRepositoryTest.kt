package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 用户 Repository 测试
 *
 * 覆盖用户的插入与按用户名查询逻辑，依赖数据库初始化的测试数据与关系
 *
 * @author lishangbu
 * @since 2025/8/20
 */
class UserRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var userRepository: UserRepository

    @Resource
    private lateinit var roleRepository: RoleRepository

    @Test
    fun testInsert() {
        val role = roleRepository.findById(1L).orElseThrow()
        val user =
            userRepository.saveAndFlush(
                User {
                    hashedPassword =
                        "{bcrypt}\$2a\$10\$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G"
                    username = "test2"
                    phone = "13800000001"
                    email = "test2@example.com"
                    avatar = "https://example.com/avatar/test2.png"
                    roles().addBy(role)
                },
            )
        assertNotNull(user.id)

        val userOptional = userRepository.findUserWithRolesByAccount("13800000001")
        assertTrue(userOptional.isPresent)
        val savedUser = userOptional.get()
        assertEquals("test2", savedUser.username)
        assertEquals("https://example.com/avatar/test2.png", savedUser.avatar)
        assertTrue(savedUser.hashedPassword!!.startsWith("{bcrypt}"))
        assertEquals(1, savedUser.roles.size)
    }

    @Test
    fun testFindByUsername() {
        val userOptional = userRepository.findUserWithRolesByAccount("admin")
        assertTrue(userOptional.isPresent)
        val user = userOptional.get()
        assertEquals("admin", user.username)
        assertEquals("https://example.com/avatar/admin.png", user.avatar)
        assertTrue(user.hashedPassword!!.startsWith("{bcrypt}"))
        assertEquals(2, user.roles.size)
    }
}
