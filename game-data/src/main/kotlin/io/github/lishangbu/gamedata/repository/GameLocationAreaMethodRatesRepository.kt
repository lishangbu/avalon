package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameLocationAreaMethodRates
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 区域遭遇方式概率持久化访问。
 */
interface GameLocationAreaMethodRatesRepository : KRepository<GameLocationAreaMethodRates, Long>
