package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameContestTypes
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 评分类别持久化访问。
 */
interface GameContestTypesRepository : KRepository<GameContestTypes, Long>
