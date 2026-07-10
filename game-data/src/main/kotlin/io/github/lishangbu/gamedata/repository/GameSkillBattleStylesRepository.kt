package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillBattleStyles
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能战斗风格持久化访问。
 */
interface GameSkillBattleStylesRepository : KRepository<GameSkillBattleStyles, Long>
