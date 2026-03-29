package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveAilmentInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveAilmentInput

/** 招式异常服务 */
interface MoveAilmentService {
    fun save(command: SaveMoveAilmentInput): MoveAilmentView

    fun update(command: UpdateMoveAilmentInput): MoveAilmentView

    fun removeById(id: Long)

    fun listByCondition(specification: MoveAilmentSpecification): List<MoveAilmentView>
}
