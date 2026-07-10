package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameGenderEvolutionRequirements
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 性别进化要求持久化访问。
 */
interface GameGenderEvolutionRequirementsRepository : KRepository<GameGenderEvolutionRequirements, Long>
