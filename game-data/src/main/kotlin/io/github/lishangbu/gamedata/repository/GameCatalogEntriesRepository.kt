package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCatalogEntries
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 图鉴目录条目持久化访问。
 */
interface GameCatalogEntriesRepository : KRepository<GameCatalogEntries, Long>
