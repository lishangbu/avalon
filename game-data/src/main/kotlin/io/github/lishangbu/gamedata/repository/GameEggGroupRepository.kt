package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEggGroup
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 种类分组持久化访问。
 */
interface GameEggGroupRepository : KRepository<GameEggGroup, Long>
