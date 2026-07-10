package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameElement
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 属性资料持久化访问。
 */
interface GameElementRepository : KRepository<GameElement, Long>
