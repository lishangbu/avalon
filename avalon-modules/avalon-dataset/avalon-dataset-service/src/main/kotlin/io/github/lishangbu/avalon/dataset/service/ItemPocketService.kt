package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemPocketInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemPocketInput

/** 道具口袋服务 */
interface ItemPocketService {
    fun save(command: SaveItemPocketInput): ItemPocketView

    fun update(command: UpdateItemPocketInput): ItemPocketView

    fun removeById(id: Long)

    fun listByCondition(specification: ItemPocketSpecification): List<ItemPocketView>
}
