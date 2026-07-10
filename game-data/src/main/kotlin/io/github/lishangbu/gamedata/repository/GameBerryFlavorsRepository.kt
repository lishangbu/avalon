package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameBerryFlavors
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 树果口味持久化访问。
 */
interface GameBerryFlavorsRepository : KRepository<GameBerryFlavors, Long>
