package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameHabitat
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 栖息地持久化访问。
 */
interface GameHabitatRepository : KRepository<GameHabitat, Long>
