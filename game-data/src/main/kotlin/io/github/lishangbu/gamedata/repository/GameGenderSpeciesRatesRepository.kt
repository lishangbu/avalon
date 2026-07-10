package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameGenderSpeciesRates
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 性别种类比例持久化访问。
 */
interface GameGenderSpeciesRatesRepository : KRepository<GameGenderSpeciesRates, Long>
