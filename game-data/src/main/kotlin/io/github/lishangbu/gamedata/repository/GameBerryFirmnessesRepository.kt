package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameBerryFirmnesses
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 树果硬度持久化访问。
 */
interface GameBerryFirmnessesRepository : KRepository<GameBerryFirmnesses, Long>
