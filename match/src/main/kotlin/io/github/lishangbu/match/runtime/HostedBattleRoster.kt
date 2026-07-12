package io.github.lishangbu.match.runtime

/** Match 向 Battle Session 提交的完整权威阵容，不携带 Account、Trainer 或 Match 标识。 */
data class HostedBattleRoster(val formatCode: String, val sides: List<HostedBattleSide>)
