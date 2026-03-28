package io.github.lishangbu.avalon.jimmer.support

import org.babyfish.jimmer.UnloadedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class JimmerPropertyAccessTest {
    @Test
    fun readOrNullReturnsNullForNullReceiverAndUnloadedProperty() {
        assertNull((null as ThrowingReader?).readOrNull { value() })
        assertNull(ThrowingReader().readOrNull { value() })
    }

    @Test
    fun readOrNullReturnsLoadedValue() {
        assertEquals("loaded", LoadedReader("loaded").readOrNull { value() })
    }

    @Test
    fun readOrNullRethrowsUnexpectedExceptions() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                LoadedReader("loaded").readOrNull { throw IllegalStateException("boom") }
            }

        assertEquals("boom", exception.message)
    }

    private class LoadedReader(
        private val currentValue: String,
    ) {
        fun value(): String = currentValue
    }

    private class ThrowingReader {
        fun value(): String = throw UnloadedException(ThrowingReader::class.java, "value")
    }
}
