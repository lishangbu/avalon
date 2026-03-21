package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonStat
import io.github.lishangbu.avalon.dataset.entity.PokemonStatId
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 宝可梦能力值数据访问接口
 *
 * 提供宝可梦能力值数据的CRUD操作
 *
 * @author lishangbu
 * @since 2026/2/16
 */
@Repository
interface PokemonStatRepository : KRepository<PokemonStat, PokemonStatId>
