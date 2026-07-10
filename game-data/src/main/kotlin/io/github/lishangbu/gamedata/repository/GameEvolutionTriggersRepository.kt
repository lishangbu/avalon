package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEvolutionTriggers
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 进化触发器持久化访问。
 */
interface GameEvolutionTriggersRepository : KRepository<GameEvolutionTriggers, Long>
