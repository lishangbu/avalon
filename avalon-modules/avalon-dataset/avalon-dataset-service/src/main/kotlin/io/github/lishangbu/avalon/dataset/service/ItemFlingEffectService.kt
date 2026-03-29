package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemFlingEffectInput

/** 道具投掷效果服务 */
interface ItemFlingEffectService {
    fun save(command: SaveItemFlingEffectInput): ItemFlingEffectView

    fun update(command: UpdateItemFlingEffectInput): ItemFlingEffectView

    fun removeById(id: Long)

    fun listByCondition(specification: ItemFlingEffectSpecification): List<ItemFlingEffectView>
}
