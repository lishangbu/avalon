package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameStatSkillEffects
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 数值项技能影响持久化访问。
 */
interface GameStatSkillEffectsRepository : KRepository<GameStatSkillEffects, Long>
