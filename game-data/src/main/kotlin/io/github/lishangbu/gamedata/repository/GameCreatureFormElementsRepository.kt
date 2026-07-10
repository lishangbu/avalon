package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreatureFormElements
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵形态属性持久化访问。
 */
interface GameCreatureFormElementsRepository : KRepository<GameCreatureFormElements, Long>
