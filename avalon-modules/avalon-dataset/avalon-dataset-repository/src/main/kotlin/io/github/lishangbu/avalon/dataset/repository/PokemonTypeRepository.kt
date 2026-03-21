package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonType
import io.github.lishangbu.avalon.dataset.entity.PokemonTypeId
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

/**
 * 宝可梦属性(PokemonType)数据访问层
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface PokemonTypeRepository :
    KRepository<PokemonType, PokemonTypeId>
