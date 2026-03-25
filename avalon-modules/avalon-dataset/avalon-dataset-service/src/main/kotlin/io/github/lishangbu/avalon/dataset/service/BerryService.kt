package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * 树果应用服务
 *
 * 定义树果的查询与写入操作
 */
interface BerryService {
    /** 按筛选条件分页查询树果*/
    fun getPageByCondition(
        specification: BerrySpecification,
        pageable: Pageable,
    ): Page<Berry>

    /** 创建树果 */
    fun save(berry: Berry): Berry

    /** 更新树果 */
    fun update(berry: Berry): Berry

    /** 删除指定 ID 的树果*/
    fun removeById(id: Long)
}
