package io.github.lishangbu.match.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class MatchBattleProjectionTests {
	@Test
	fun `turn command identifiers are deterministic distinct version four UUIDs`() {
		val first = stableTurnCommandId(42, 3)
		assertThat(stableTurnCommandId(42, 3)).isEqualTo(first)
		assertThat(stableTurnCommandId(42, 4)).isNotEqualTo(first)
		assertThat(UUID.fromString(first).version()).isEqualTo(4)
	}

	@Test
	fun `actor identifiers project to stable side and position`() {
		assertThat(actorSide("side-2-actor-4")).isEqualTo(2)
		assertThat(actorPosition("side-2-actor-4")).isEqualTo(4)
	}
}
