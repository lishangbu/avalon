package io.github.lishangbu.match.trainer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TrainerDisplayNameTests {
	@Test
	fun `normalizes width case and surrounding whitespace`() {
		val name = TrainerDisplayName.of("  Ａｖａｌｏｎ_一号  ")

		assertEquals("Avalon_一号", name.value)
		assertEquals("avalon_一号", name.key)
		assertEquals("avalon一号", name.moderationKey)
	}

	@Test
	fun `rejects invalid length and punctuation`() {
		assertFailsWith<InvalidTrainerDisplayNameException> { TrainerDisplayName.of("a") }
		assertFailsWith<InvalidTrainerDisplayNameException> { TrainerDisplayName.of("valid.name") }
	}
}
