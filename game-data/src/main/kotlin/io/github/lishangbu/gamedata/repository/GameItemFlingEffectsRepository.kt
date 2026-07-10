package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItemFlingEffects
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具投掷效果持久化访问。
 */
interface GameItemFlingEffectsRepository : KRepository<GameItemFlingEffects, Long>
