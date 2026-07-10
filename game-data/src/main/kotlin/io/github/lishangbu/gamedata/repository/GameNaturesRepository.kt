package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameNatures
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 性格资料持久化访问。
 */
interface GameNaturesRepository : KRepository<GameNatures, Long>
