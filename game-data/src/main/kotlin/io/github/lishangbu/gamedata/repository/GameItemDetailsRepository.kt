package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItemDetails
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具详情持久化访问。
 */
interface GameItemDetailsRepository : KRepository<GameItemDetails, Long>
