package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameAbilityDetails
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 特性详情持久化访问。
 */
interface GameAbilityDetailsRepository : KRepository<GameAbilityDetails, Long>
