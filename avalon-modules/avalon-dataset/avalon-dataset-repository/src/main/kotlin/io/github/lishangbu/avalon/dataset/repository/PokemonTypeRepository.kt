package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonType
import io.github.lishangbu.avalon.dataset.entity.PokemonTypeId
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 宝可梦属性仓储接口
 *
 * 定义宝可梦属性数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface PokemonTypeRepository : KRepository<PokemonType, PokemonTypeId>
