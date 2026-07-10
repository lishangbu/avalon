package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreatureForms
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵形态持久化访问。
 */
interface GameCreatureFormsRepository : KRepository<GameCreatureForms, Long>
