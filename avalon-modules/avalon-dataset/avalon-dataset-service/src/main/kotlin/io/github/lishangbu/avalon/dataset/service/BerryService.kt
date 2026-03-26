package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryInput
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
    ): Page<BerryView>

    /** 创建树果 */
    fun save(command: SaveBerryInput): BerryView

    /** 更新树果 */
    fun update(command: UpdateBerryInput): BerryView

    /** 删除指定 ID 的树果*/
    fun removeById(id: Long)
}
