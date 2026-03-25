package io.github.lishangbu.avalon.dataset.repository

import org.babyfish.jimmer.UnloadedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RepositorySupportTest {
    @Test
    fun takeFilterTrimsAndDropsBlankValues() {
        assertNull((null as String?).takeFilter())
        assertNull("   ".takeFilter())
        assertEquals("value", "  value  ".takeFilter())
    }

    @Test
    fun readOrNullReturnsNullForNullReceiverAndUnloadedException() {
        assertNull((null as ThrowingReader?).readOrNull { value() })
        assertNull(ThrowingReader().readOrNull { value() })
    }

    @Test
    fun readOrNullReturnsLoadedValue() {
        val result = LoadedReader("loaded").readOrNull { value() }

        assertEquals("loaded", result)
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
