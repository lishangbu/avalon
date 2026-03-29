package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonShapeInput

/** 宝可梦形状服务 */
interface PokemonShapeService {
    /** 创建宝可梦形状 */
    fun save(command: SavePokemonShapeInput): PokemonShapeView

    /** 更新宝可梦形状 */
    fun update(command: UpdatePokemonShapeInput): PokemonShapeView

    /** 删除指定 ID 的宝可梦形状 */
    fun removeById(id: Long)

    /** 按筛选条件查询宝可梦形状列表 */
    fun listByCondition(specification: PokemonShapeSpecification): List<PokemonShapeView>
}
