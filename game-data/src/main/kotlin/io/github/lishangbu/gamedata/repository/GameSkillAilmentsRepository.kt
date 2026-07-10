package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillAilments
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能异常持久化访问。
 */
interface GameSkillAilmentsRepository : KRepository<GameSkillAilments, Long>
