package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameGrowthRateLevels
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 成长等级经验持久化访问。
 */
interface GameGrowthRateLevelsRepository : KRepository<GameGrowthRateLevels, Long>
