package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillDetails
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能详情持久化访问。
 */
interface GameSkillDetailsRepository : KRepository<GameSkillDetails, Long>
