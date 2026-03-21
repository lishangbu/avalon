package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.Stat
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 能力(Stat)服务。 */
interface StatService {
    /** 根据条件分页查询能力。 */
    fun getPageByCondition(
        stat: Stat,
        pageable: Pageable,
    ): Page<Stat>

    /** 新增能力。 */
    fun save(stat: Stat): Stat

    /** 更新能力。 */
    fun update(stat: Stat): Stat

    /** 根据主键删除能力。 */
    fun removeById(id: Long)

    /** 根据条件查询能力列表。 */
    fun listByCondition(stat: Stat): List<Stat>
}
