package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEvolutionChains
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 进化链持久化访问。
 */
interface GameEvolutionChainsRepository : KRepository<GameEvolutionChains, Long>
