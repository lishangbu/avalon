package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreatureStat
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵数值绑定持久化访问。
 */
interface GameCreatureStatRepository : KRepository<GameCreatureStat, Long>
