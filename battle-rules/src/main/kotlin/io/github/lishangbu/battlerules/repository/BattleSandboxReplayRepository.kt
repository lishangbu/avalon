package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleSandboxReplay
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗沙盒复盘记录的 Jimmer Repository。
 */
interface BattleSandboxReplayRepository : KRepository<BattleSandboxReplay, Long>
