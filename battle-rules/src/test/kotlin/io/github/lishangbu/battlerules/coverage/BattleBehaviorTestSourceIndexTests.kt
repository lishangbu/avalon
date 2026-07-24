package io.github.lishangbu.battlerules.coverage

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

/** 验证行为测试源码索引只把可执行效果构造登记为覆盖证据。 */
class BattleBehaviorTestSourceIndexTests {
	@Test
	fun `comments and strings do not count as item effect construction`(@TempDir projectRoot: Path) {
		val sourceRoot = projectRoot.resolve("battle-engine/src/test/kotlin")
		Files.createDirectories(sourceRoot)
		Files.writeString(
			sourceRoot.resolve("ActualItemTests.kt"),
			"@Test fun behavior() { val effect = BattleItemEffect.CreatureDamageBoost(1.2) }",
		)
		Files.writeString(
			sourceRoot.resolve("CommentOnlyTests.kt"),
			"@Test fun behavior() { // BattleItemEffect.CreatureDamageBoost(1.2)\n }",
		)
		Files.writeString(
			sourceRoot.resolve("StringOnlyTests.kt"),
			"@Test fun behavior() { val example = \"BattleItemEffect.CreatureDamageBoost(1.2)\" }",
		)

		val index = BattleBehaviorTestSourceIndex(projectRoot)

		assertEquals(
			setOf("ActualItemTests"),
			index.classesInstantiating("BattleItemEffect", "CreatureDamageBoost"),
		)
	}
}
