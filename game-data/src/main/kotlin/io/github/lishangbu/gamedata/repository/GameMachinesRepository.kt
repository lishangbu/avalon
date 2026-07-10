package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameMachines
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 机器资料持久化访问。
 */
interface GameMachinesRepository : KRepository<GameMachines, Long>
