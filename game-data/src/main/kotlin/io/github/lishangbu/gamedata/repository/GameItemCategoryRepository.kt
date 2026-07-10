package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItemCategory
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具分类持久化访问。
 */
interface GameItemCategoryRepository : KRepository<GameItemCategory, Long>
