package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 能力值仓储接口
 *
 * 定义能力值数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/11
 */
interface StatRepository :
    KRepository<Stat, Long>,
    StatRepositoryExt

interface StatRepositoryExt {
    /** 按条件查询能力值列表 */
    fun findAll(specification: StatSpecification?): List<StatView>

    /** 按 ID 查询能力值 */
    fun findViewById(id: Long): StatView?

    /** 按 ID 删除能力值 */
    fun removeById(id: Long)
}
