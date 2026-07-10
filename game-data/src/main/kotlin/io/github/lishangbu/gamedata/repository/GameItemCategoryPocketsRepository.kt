package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItemCategoryPockets
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具分类口袋持久化访问。
 */
interface GameItemCategoryPocketsRepository : KRepository<GameItemCategoryPockets, Long>
