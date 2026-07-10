package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItemAttributeBindings
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具属性绑定持久化访问。
 */
interface GameItemAttributeBindingsRepository : KRepository<GameItemAttributeBindings, Long>
