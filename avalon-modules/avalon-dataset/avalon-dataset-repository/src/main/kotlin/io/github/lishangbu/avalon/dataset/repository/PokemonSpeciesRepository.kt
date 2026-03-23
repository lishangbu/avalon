package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 宝可梦种类仓储接口
 *
 * 定义宝可梦种类数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/12
 */
@Repository
interface PokemonSpeciesRepository : KRepository<PokemonSpecies, Long>
