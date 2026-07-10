package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameStatNatureEffects
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 数值项性格影响持久化访问。
 */
interface GameStatNatureEffectsRepository : KRepository<GameStatNatureEffects, Long>
