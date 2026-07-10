package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreatureElement
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵属性绑定持久化访问。
 */
interface GameCreatureElementRepository : KRepository<GameCreatureElement, Long>
