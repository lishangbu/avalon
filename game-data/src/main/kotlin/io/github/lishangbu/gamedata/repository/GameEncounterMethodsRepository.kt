package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEncounterMethods
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 遭遇方式持久化访问。
 */
interface GameEncounterMethodsRepository : KRepository<GameEncounterMethods, Long>
