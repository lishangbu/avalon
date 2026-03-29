package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemCategoryInput

/** 道具类别服务 */
interface ItemCategoryService {
    fun save(command: SaveItemCategoryInput): ItemCategoryView

    fun update(command: UpdateItemCategoryInput): ItemCategoryView

    fun removeById(id: Long)

    fun listByCondition(specification: ItemCategorySpecification): List<ItemCategoryView>
}
