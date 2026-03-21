package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.Berry
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 树果服务。 */
interface BerryService {
    /** 根据条件分页查询树果。 */
    fun getPageByCondition(
        berry: Berry,
        pageable: Pageable,
    ): Page<Berry>

    /** 新增树果。 */
    fun save(berry: Berry): Berry

    /** 更新树果。 */
    fun update(berry: Berry): Berry

    /** 根据主键删除树果。 */
    fun removeById(id: Long)
}
