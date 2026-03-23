package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 招式伤害分类仓储接口
 *
 * 定义招式伤害分类数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface MoveDamageClassRepository {
    /** 按条件查询招式伤害分类列表 */
    fun findAll(example: Example<MoveDamageClass>?): List<MoveDamageClass>

    /** 按条件分页查询招式伤害分类 */
    fun findAll(
        example: Example<MoveDamageClass>?,
        pageable: Pageable,
    ): Page<MoveDamageClass>

    /** 保存招式伤害分类 */
    fun save(moveDamageClass: MoveDamageClass): MoveDamageClass

    /** 按 ID 删除招式伤害分类 */
    fun deleteById(id: Long)
}
