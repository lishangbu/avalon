package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.data.domain.Pageable

/**
 * 招式伤害分类仓储接口
 *
 * 定义招式伤害分类数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface MoveDamageClassRepository :
    KRepository<MoveDamageClass, Long>,
    MoveDamageClassRepositoryExt

interface MoveDamageClassRepositoryExt {
    /** 按条件查询招式伤害分类列表 */
    fun findAll(specification: MoveDamageClassSpecification?): List<MoveDamageClass>

    /** 按条件分页查询招式伤害分类 */
    fun findAll(
        specification: MoveDamageClassSpecification?,
        pageable: Pageable,
    ): Page<MoveDamageClass>

    /** 按 ID 删除招式伤害分类 */
    fun removeById(id: Long)
}
