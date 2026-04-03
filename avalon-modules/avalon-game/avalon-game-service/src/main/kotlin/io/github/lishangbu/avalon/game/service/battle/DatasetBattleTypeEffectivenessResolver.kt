package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessService
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleTypeEffectivenessResolver
import org.springframework.stereotype.Component

/**
 * 基于 dataset 类型相性服务的 battle engine 属性克制解析器。
 */
@Component
class DatasetBattleTypeEffectivenessResolver(
    private val typeEffectivenessService: TypeEffectivenessService,
) : BattleTypeEffectivenessResolver {
    override fun resolve(
        moveType: String?,
        attacker: UnitState?,
        target: UnitState?,
    ): Double {
        if (moveType == null || target == null || target.typeIds.isEmpty()) {
            return 1.0
        }
        return typeEffectivenessService.calculate(moveType, target.typeIds.toList()).finalMultiplier?.toDouble() ?: 1.0
    }
}
