package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSpeciesShape
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 种类形态持久化访问。
 */
interface GameSpeciesShapeRepository : KRepository<GameSpeciesShape, Long>
