package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 默认 battle session 事件发布器。
 *
 * @property projectors 按顺序执行的 projector 集合。
 */
class DefaultBattleSessionEventPublisher(
    projectors: List<BattleSessionProjector>,
) : BattleSessionEventPublisher {
    private val projectors: List<BattleSessionProjector> = projectors.sortedBy(BattleSessionProjector::order)

    /**
     * 把一条 session 内部事件发布给全部 projector。
     */
    override fun publish(
        session: BattleSession,
        publication: BattleSessionPublication,
    ) {
        projectors.forEach { projector ->
            projector.project(session, publication)
        }
    }
}
