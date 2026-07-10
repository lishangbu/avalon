package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameElementDamageRelations
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 属性克制关系持久化访问。
 */
interface GameElementDamageRelationsRepository : KRepository<GameElementDamageRelations, Long>
