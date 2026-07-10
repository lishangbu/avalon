package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSpeciesCatalogNumbers
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 种类目录编号持久化访问。
 */
interface GameSpeciesCatalogNumbersRepository : KRepository<GameSpeciesCatalogNumbers, Long>
