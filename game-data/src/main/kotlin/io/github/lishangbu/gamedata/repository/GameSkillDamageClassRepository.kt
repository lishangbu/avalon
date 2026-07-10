package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillDamageClass
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能分类持久化访问。
 */
interface GameSkillDamageClassRepository : KRepository<GameSkillDamageClass, Long>
