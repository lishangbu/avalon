package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameLocationAreaEncounters
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 区域精灵遭遇持久化访问。
 */
interface GameLocationAreaEncountersRepository : KRepository<GameLocationAreaEncounters, Long>
