package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemAttributeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemAttributeInput

/** 道具属性服务 */
interface ItemAttributeService {
    fun save(command: SaveItemAttributeInput): ItemAttributeView

    fun update(command: UpdateItemAttributeInput): ItemAttributeView

    fun removeById(id: Long)

    fun listByCondition(specification: ItemAttributeSpecification): List<ItemAttributeView>
}
