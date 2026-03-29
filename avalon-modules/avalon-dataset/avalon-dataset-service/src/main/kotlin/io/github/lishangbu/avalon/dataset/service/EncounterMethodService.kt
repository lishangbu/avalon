package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterMethodInput

/** 遭遇方式服务 */
interface EncounterMethodService {
    /** 创建遭遇方式 */
    fun save(command: SaveEncounterMethodInput): EncounterMethodView

    /** 更新遭遇方式 */
    fun update(command: UpdateEncounterMethodInput): EncounterMethodView

    /** 删除指定 ID 的遭遇方式 */
    fun removeById(id: Long)

    /** 按筛选条件查询遭遇方式列表 */
    fun listByCondition(specification: EncounterMethodSpecification): List<EncounterMethodView>
}
