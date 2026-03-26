package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 仓储扩展函数测试
 *
 * 验证 `readOrNull` 对未加载属性与异常场景的处理
 */
class RepositorySupportTest {
    /** 验证 takeFilter 会裁剪空白并过滤空字符串 */
    @Test
    fun trimsAndFiltersBlankStrings() {
        assertNull(null.takeFilter())
        assertNull("   ".takeFilter())
        assertEquals("ash", "  ash  ".takeFilter())
    }

    /** 验证未加载属性会返回 null */
    @Test
    fun returnsNullForUnloadedProperty() {
        val user =
            User {
                id = 1L
            }

        assertNull(user.readOrNull { username })
    }

    /** 验证非预期异常会继续向外抛出 */
    @Test
    fun rethrowsUnexpectedExceptions() {
        val user =
            User {
                id = 1L
                username = "ash"
            }

        val exception =
            assertThrows(IllegalStateException::class.java) {
                user.readOrNull { throw IllegalStateException("boom") }
            }

        assertEquals("boom", exception.message)
    }
}
