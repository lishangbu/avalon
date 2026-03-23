package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonStat
import io.github.lishangbu.avalon.dataset.entity.PokemonStatId
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 宝可梦能力值仓储接口
 *
 * 定义宝可梦能力值数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/16
 */
@Repository
interface PokemonStatRepository : KRepository<PokemonStat, PokemonStatId>
