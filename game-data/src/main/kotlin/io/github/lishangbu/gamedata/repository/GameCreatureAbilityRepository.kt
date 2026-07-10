package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreatureAbility
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵特性绑定持久化访问。
 */
interface GameCreatureAbilityRepository : KRepository<GameCreatureAbility, Long>
