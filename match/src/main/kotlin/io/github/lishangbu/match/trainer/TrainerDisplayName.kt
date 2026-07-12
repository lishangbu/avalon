package io.github.lishangbu.match.trainer

import java.text.Normalizer
import java.util.Locale

@ConsistentCopyVisibility
data class TrainerDisplayName private constructor(
	val value: String,
	val key: String,
	val moderationKey: String,
) {
	companion object {
		private val allowed = Regex("^[\\p{L}\\p{N} _-]+$")

		fun of(input: String): TrainerDisplayName {
			val value = Normalizer.normalize(input.trim(), Normalizer.Form.NFKC)
			val key = value.lowercase(Locale.ROOT)
			val length = key.codePointCount(0, key.length)
			if (length !in 2..16 || !allowed.matches(key)) {
				throw InvalidTrainerDisplayNameException()
			}
			return TrainerDisplayName(value, key, key.replace(Regex("[ _-]"), ""))
		}
	}
}
