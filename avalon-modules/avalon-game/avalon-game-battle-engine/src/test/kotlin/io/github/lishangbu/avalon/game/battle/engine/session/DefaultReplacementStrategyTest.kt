package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultReplacementStrategy` 是否保持“第一个可用 bench 顶上”的默认语义。
 */
class DefaultReplacementStrategyTest {
    private val strategy = DefaultReplacementStrategy()

    /**
     * 验证：
     * - 当 active 倒下后，默认策略会按 unitIds 顺序挑选第一个仍存活的 bench 顶上。
     */
    @Test
    fun shouldSelectFirstAvailableBenchUnitWhenActiveUnitFaints() {
        val side =
            SideState(
                id = "p2",
                unitIds = listOf("p2a", "p2b", "p2c"),
                activeUnitIds = listOf("p2a"),
            )
        val units =
            mapOf(
                "p2a" to UnitState(id = "p2a", currentHp = 0, maxHp = 100),
                "p2b" to UnitState(id = "p2b", currentHp = 100, maxHp = 100),
                "p2c" to UnitState(id = "p2c", currentHp = 100, maxHp = 100),
            )

        val activeIds = strategy.selectActiveUnitIds(side, units)

        assertEquals(listOf("p2b"), activeIds)
    }
}
