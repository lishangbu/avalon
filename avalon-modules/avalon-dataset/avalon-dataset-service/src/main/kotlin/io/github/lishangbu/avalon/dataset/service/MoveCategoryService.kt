package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveCategoryInput

/** 招式类别服务 */
interface MoveCategoryService {
    fun save(command: SaveMoveCategoryInput): MoveCategoryView

    fun update(command: UpdateMoveCategoryInput): MoveCategoryView

    fun removeById(id: Long)

    fun listByCondition(specification: MoveCategorySpecification): List<MoveCategoryView>
}
