package io.github.lishangbu.avalon.game.battle.engine.loader

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `JsonEffectDefinitionBattleDataLoader` 是否能稳定加载当前真实 fixture 样例库。
 *
 * 这类测试不验证 battle 运行时行为，而是保证样例资源本身始终可被解析成 `EffectDefinition`。
 */
class JsonEffectDefinitionBattleDataLoaderTest {
    /**
     * 验证：
     * - 当前测试资源目录中的真实 CSV 改造样例都能被 loader 成功读取。
     * - 每个样例都能落到预期的 hook 上，说明 JSON -> EffectDefinition 的映射没有跑偏。
     *
     * 这个测试的意义在于保证样例库本身始终可加载。
     * 一旦这里失败，后续所有依赖 fixture 的规则处理和 battle flow 测试都不可信。
     */
    @Test
    fun shouldLoadRealEffectFixturesWhenReadingAdaptedCsvResources() {
        val loader =
            JsonEffectDefinitionBattleDataLoader(
                resourcePaths =
                    listOf(
                        "data/battle/fixtures/move/thunderbolt.json",
                        "data/battle/fixtures/move/ember.json",
                        "data/battle/fixtures/ability/speed-boost.json",
                        "data/battle/fixtures/ability/guts.json",
                        "data/battle/fixtures/move/surf-rain.json",
                        "data/battle/fixtures/move/recover.json",
                        "data/battle/fixtures/move/haze.json",
                        "data/battle/fixtures/move/soak.json",
                        "data/battle/fixtures/move/thunder-wave.json",
                        "data/battle/fixtures/move/will-o-wisp.json",
                        "data/battle/fixtures/move/swords-dance.json",
                        "data/battle/fixtures/ability/static.json",
                        "data/battle/fixtures/move/feather-dance.json",
                        "data/battle/fixtures/move/teeter-dance.json",
                        "data/battle/fixtures/move/refresh.json",
                        "data/battle/fixtures/item/sitrus-berry.json",
                        "data/battle/fixtures/ability/tangled-feet.json",
                        "data/battle/fixtures/move/electro-ball.json",
                        "data/battle/fixtures/ability/synchronize.json",
                        "data/battle/fixtures/move/supersonic.json",
                        "data/battle/fixtures/move/thunder-wave-immunity.json",
                        "data/battle/fixtures/ability/limber.json",
                        "data/battle/fixtures/ability/water-veil.json",
                        "data/battle/fixtures/ability/own-tempo.json",
                        "data/battle/fixtures/ability/adaptability.json",
                        "data/battle/fixtures/move/slash.json",
                        "data/battle/fixtures/ability/super-luck.json",
                        "data/battle/fixtures/ability/battle-armor.json",
                    ),
            )

        val effects = loader.loadEffects()

        assertEquals(28, effects.size)
        assertTrue(effects.any { effect -> effect.id == "thunderbolt" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "ember" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "speed-boost" && StandardHookNames.ON_RESIDUAL in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "guts" && StandardHookNames.ON_MODIFY_ATTACK in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "surf" && StandardHookNames.ON_MODIFY_DAMAGE in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "recover" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "haze" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "soak" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "thunder-wave" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "will-o-wisp" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "swords-dance" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "static" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "feather-dance" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "teeter-dance" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "refresh" && StandardHookNames.ON_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "sitrus-berry" && StandardHookNames.ON_RESIDUAL in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "tangled-feet" && StandardHookNames.ON_MODIFY_EVASION in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "electro-ball" && StandardHookNames.ON_MODIFY_BASE_POWER in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "synchronize" && StandardHookNames.ON_AFTER_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "supersonic" && StandardHookNames.ON_PREPARE_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "thunder-wave-immunity" && StandardHookNames.ON_TRY_HIT in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "limber" && StandardHookNames.ON_SET_STATUS in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "water-veil" && StandardHookNames.ON_SET_STATUS in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "own-tempo" && StandardHookNames.ON_TRY_ADD_VOLATILE in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "adaptability" && StandardHookNames.ON_MODIFY_STAB in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "slash" && effect.data["critRatio"] == 1 })
        assertTrue(effects.any { effect -> effect.id == "super-luck" && StandardHookNames.ON_MODIFY_CRIT_RATIO in effect.hooks.keys })
        assertTrue(effects.any { effect -> effect.id == "battle-armor" && StandardHookNames.ON_MODIFY_CRIT_RATIO in effect.hooks.keys })
    }
}
