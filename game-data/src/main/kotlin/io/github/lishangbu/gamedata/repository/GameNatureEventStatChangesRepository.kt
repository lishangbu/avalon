package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameNatureEventStatChanges
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 性格活动能力变化持久化访问。
 */
interface GameNatureEventStatChangesRepository : KRepository<GameNatureEventStatChanges, Long>
