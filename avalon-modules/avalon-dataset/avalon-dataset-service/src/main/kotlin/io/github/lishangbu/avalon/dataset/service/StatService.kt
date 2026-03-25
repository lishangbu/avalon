package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification

/** 能力值服务*/
interface StatService {
    /** 保存能力值*/
    fun save(stat: Stat): Stat

    /** 更新能力值*/
    fun update(stat: Stat): Stat

    /** 按 ID 删除能力值*/
    fun removeById(id: Long)

    /** 根据条件查询能力值列表*/
    fun listByCondition(specification: StatSpecification): List<Stat>
}
