package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonAbility
import io.github.lishangbu.avalon.dataset.entity.PokemonAbilityId
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 宝可梦特性仓储接口
 *
 * 定义宝可梦特性数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Repository
interface PokemonAbilityRepository : KRepository<PokemonAbility, PokemonAbilityId>
