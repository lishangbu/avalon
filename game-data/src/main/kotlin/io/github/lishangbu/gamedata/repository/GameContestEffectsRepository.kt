package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameContestEffects
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 评价效果持久化访问。
 */
interface GameContestEffectsRepository : KRepository<GameContestEffects, Long>
