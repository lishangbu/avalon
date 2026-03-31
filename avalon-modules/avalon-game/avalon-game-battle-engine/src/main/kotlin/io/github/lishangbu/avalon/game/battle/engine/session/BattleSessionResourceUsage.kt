package io.github.lishangbu.avalon.game.battle.engine.session

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * battle session 内部资源账本条目。
 *
 * 设计意图：
 * - 用强类型账本条目替代字符串化的 `actionType` 分支。
 * - 让不同资源消耗场景可以通过独立类型扩展，而不是继续堆可空字段。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
interface BattleSessionResourceUsage {
    /**
     * 当前账本条目的业务种类。
     */
    val kind: BattleSessionResourceUsageKind
}
