package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
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
        val role = requireNotNull(roleRepository.findNullable(1L))
        val user =
            userRepository.save(
                User {
                    hashedPassword =
                        "{bcrypt}\$2a\$10\$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G"
                    username = "test2"
                    phone = "13800000001"
                    email = "test2@example.com"
                    avatar = "https://example.com/avatar/test2.png"
                    roles().addBy(role)
                },
                SaveMode.INSERT_ONLY,
            )
        assertNotNull(user.id)

        val savedUser = requireNotNull(userRepository.loadByAccountWithRoles("13800000001"))
        assertEquals("test2", savedUser.username)
        assertEquals("https://example.com/avatar/test2.png", savedUser.avatar)
        assertTrue(savedUser.hashedPassword!!.startsWith("{bcrypt}"))
        assertEquals(1, savedUser.roles.size)
    }

    @Test
    fun testFindByIdDoesNotFetchRolesByDefault() {
        val user = requireNotNull(userRepository.findNullable(1L))
        assertNull(user.readOrNull { roles })
    }

    @Test
    fun testFindByIdWithRoles() {
        val user = requireNotNull(userRepository.findNullable(1L, AuthorizationFetchers.USER_WITH_ROLES))
        assertEquals(2, user.roles.size)
    }

    @Test
    fun testFindByAccountWithRoles() {
        val user = requireNotNull(userRepository.loadByAccountWithRoles("admin"))
        assertEquals("admin", user.username)
        assertNull(user.avatar)
        assertTrue(user.hashedPassword!!.startsWith("{bcrypt}"))
        assertEquals(2, user.roles.size)
    }
}
