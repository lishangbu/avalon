package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSkillCategories
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能元分类持久化访问。
 */
interface GameSkillCategoriesRepository : KRepository<GameSkillCategories, Long>
