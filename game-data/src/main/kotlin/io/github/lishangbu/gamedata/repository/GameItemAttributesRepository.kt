package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItemAttributes
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具属性持久化访问。
 */
interface GameItemAttributesRepository : KRepository<GameItemAttributes, Long>
