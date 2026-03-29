package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveTargetInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveTargetInput

/** 招式目标服务 */
interface MoveTargetService {
    fun save(command: SaveMoveTargetInput): MoveTargetView

    fun update(command: UpdateMoveTargetInput): MoveTargetView

    fun removeById(id: Long)

    fun listByCondition(specification: MoveTargetSpecification): List<MoveTargetView>
}
