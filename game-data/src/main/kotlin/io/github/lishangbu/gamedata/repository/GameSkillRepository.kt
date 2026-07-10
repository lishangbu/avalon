package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkill
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能资料持久化访问。
 */
interface GameSkillRepository : KRepository<GameSkill, Long>
