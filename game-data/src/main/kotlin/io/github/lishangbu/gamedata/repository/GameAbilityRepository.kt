package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameAbility
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 特性资料持久化访问。
 */
interface GameAbilityRepository : KRepository<GameAbility, Long>
