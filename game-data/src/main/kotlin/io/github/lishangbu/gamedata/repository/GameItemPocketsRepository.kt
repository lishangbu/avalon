package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItemPockets
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具口袋持久化访问。
 */
interface GameItemPocketsRepository : KRepository<GameItemPockets, Long>
