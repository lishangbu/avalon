package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveLearnMethodInput

/** 招式学习方式服务 */
interface MoveLearnMethodService {
    fun save(command: SaveMoveLearnMethodInput): MoveLearnMethodView

    fun update(command: UpdateMoveLearnMethodInput): MoveLearnMethodView

    fun removeById(id: Long)

    fun listByCondition(specification: MoveLearnMethodSpecification): List<MoveLearnMethodView>
}
