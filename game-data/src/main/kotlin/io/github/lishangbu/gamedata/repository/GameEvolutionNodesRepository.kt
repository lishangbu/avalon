package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEvolutionNodes
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 进化链节点持久化访问。
 */
interface GameEvolutionNodesRepository : KRepository<GameEvolutionNodes, Long>
