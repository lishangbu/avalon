package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameAdvancedContestEffects
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 高级评价效果持久化访问。
 */
interface GameAdvancedContestEffectsRepository : KRepository<GameAdvancedContestEffects, Long>
