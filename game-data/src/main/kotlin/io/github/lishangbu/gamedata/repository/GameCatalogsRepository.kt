package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCatalogs
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 图鉴目录持久化访问。
 */
interface GameCatalogsRepository : KRepository<GameCatalogs, Long>
