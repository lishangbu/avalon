package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillLearnMethods
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能学习方式持久化访问。
 */
interface GameSkillLearnMethodsRepository : KRepository<GameSkillLearnMethods, Long>
