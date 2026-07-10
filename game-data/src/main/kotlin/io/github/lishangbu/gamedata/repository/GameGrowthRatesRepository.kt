package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameGrowthRates
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 成长速率持久化访问。
 */
interface GameGrowthRatesRepository : KRepository<GameGrowthRates, Long>
