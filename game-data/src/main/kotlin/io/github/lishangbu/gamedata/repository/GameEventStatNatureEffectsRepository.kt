package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEventStatNatureEffects
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 活动能力性格影响持久化访问。
 */
interface GameEventStatNatureEffectsRepository : KRepository<GameEventStatNatureEffects, Long>
