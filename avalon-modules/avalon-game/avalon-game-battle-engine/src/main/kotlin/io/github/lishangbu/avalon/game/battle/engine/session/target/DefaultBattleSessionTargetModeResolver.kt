package io.github.lishangbu.avalon.game.battle.engine.session.target

import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository

/**
 * 默认 effect 目标模式解析器。
 *
 * @property effectRepository effect 定义查询入口。
 */
class DefaultBattleSessionTargetModeResolver(
    private val effectRepository: EffectDefinitionRepository,
) : BattleSessionTargetModeResolver {
    /**
     * 解析指定 effect 的目标模式。
     */
    override fun resolve(effectId: String): BattleSessionTargetMode =
        BattleSessionTargetingSupport.resolveMode(
            effectRepository.get(effectId).data["target"]?.toString(),
        )
}
