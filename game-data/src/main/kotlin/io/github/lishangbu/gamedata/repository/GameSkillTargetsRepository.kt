package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillTargets
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能目标持久化访问。
 */
interface GameSkillTargetsRepository : KRepository<GameSkillTargets, Long>
