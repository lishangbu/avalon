package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameNatureBattleStylePreferences
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 性格战斗风格偏好持久化访问。
 */
interface GameNatureBattleStylePreferencesRepository : KRepository<GameNatureBattleStylePreferences, Long>
