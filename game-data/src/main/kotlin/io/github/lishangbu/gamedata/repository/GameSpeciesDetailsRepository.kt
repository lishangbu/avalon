package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSpeciesDetails
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 种类详情持久化访问。
 */
interface GameSpeciesDetailsRepository : KRepository<GameSpeciesDetails, Long>
