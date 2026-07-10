package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillStatChanges
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能数值变化持久化访问。
 */
interface GameSkillStatChangesRepository : KRepository<GameSkillStatChanges, Long>
