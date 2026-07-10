package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreatureSkillLearns
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵技能学习持久化访问。
 */
interface GameCreatureSkillLearnsRepository : KRepository<GameCreatureSkillLearns, Long>
