package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.data.domain.Pageable

/**
 * 树果仓储接口
 *
 * 定义树果的查询、保存与删除操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryRepository :
    KRepository<Berry, Long>,
    BerryRepositoryExt

interface BerryRepositoryExt {
    /** 按条件查询树果列表 */
    fun findAll(specification: BerrySpecification?): List<Berry>

    /** 按条件分页查询树果 */
    fun findAll(
        specification: BerrySpecification?,
        pageable: Pageable,
    ): Page<Berry>

    /** 按 ID 查询单个树果及其关联 */
    fun findByIdWithAssociations(id: Long): Berry?

    /** 删除指定 ID 的树果 */
    fun removeById(id: Long)
}
