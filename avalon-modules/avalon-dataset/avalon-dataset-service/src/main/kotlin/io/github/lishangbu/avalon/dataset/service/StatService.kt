package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.SaveStatInput
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput

/** 能力值服务*/
interface StatService {
    /** 保存能力值*/
    fun save(command: SaveStatInput): StatView

    /** 更新能力值*/
    fun update(command: UpdateStatInput): StatView

    /** 按 ID 删除能力值*/
    fun removeById(id: Long)

    /** 根据条件查询能力值列表*/
    fun listByCondition(specification: StatSpecification): List<StatView>
}
