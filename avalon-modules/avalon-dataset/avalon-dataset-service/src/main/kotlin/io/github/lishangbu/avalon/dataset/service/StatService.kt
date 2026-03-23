package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.Stat
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 能力值服务*/
interface StatService {
    /** 按条件分页查询能力值*/
    fun getPageByCondition(
        stat: Stat,
        pageable: Pageable,
    ): Page<Stat>

    /** 保存能力值*/
    fun save(stat: Stat): Stat

    /** 更新能力值*/
    fun update(stat: Stat): Stat

    /** 按 ID 删除能力值*/
    fun removeById(id: Long)

    /** 根据条件查询能力值列表*/
    fun listByCondition(stat: Stat): List<Stat>
}
