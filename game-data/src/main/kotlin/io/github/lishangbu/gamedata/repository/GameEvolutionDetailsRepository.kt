package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEvolutionDetails
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 进化条件持久化访问。
 */
interface GameEvolutionDetailsRepository : KRepository<GameEvolutionDetails, Long>
