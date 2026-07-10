package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameBerryFlavorPotencies
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 树果口味强度持久化访问。
 */
interface GameBerryFlavorPotenciesRepository : KRepository<GameBerryFlavorPotencies, Long>
