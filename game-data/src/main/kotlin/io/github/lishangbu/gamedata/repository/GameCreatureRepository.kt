package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreature
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵资料持久化访问。
 */
interface GameCreatureRepository : KRepository<GameCreature, Long>
