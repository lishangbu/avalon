package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillContestCombos
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能评价组合持久化访问。
 */
interface GameSkillContestCombosRepository : KRepository<GameSkillContestCombos, Long>
