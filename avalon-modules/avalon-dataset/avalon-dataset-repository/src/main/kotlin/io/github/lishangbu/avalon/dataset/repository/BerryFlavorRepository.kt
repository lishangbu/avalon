package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 树果风味仓储接口
 *
 * 定义树果风味数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFlavorRepository :
    KRepository<BerryFlavor, Long>,
    BerryFlavorRepositoryExt

/** 树果风味仓储扩展接口 */
interface BerryFlavorRepositoryExt {
    /** 按条件查询树果风味列表 */
    fun findAll(specification: BerryFlavorSpecification?): List<BerryFlavor>

    /** 按 ID 删除树果风味 */
    fun removeById(id: Long)
}
