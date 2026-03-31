package io.github.lishangbu.avalon.game.battle.engine.runtime.support

import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.type.ActorId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardActorIds

/**
 * 从 EventContext 中解析 actor 的辅助组件。
 */
object EventContextActorReader {
    fun readUnit(
        actor: ActorId,
        context: EventContext,
    ): UnitState? =
        when (actor) {
            StandardActorIds.SELF -> context.self
            StandardActorIds.TARGET -> context.target
            StandardActorIds.SOURCE -> context.source
            else -> null
        }
}
