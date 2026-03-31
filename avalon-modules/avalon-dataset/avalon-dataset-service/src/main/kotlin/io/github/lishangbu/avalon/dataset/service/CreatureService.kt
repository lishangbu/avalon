package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 生物应用服务 */
interface CreatureService {
    /** 按筛选条件分页查询生物 */
    fun getPageByCondition(
        specification: CreatureSpecification,
        pageable: Pageable,
    ): Page<CreatureView>

    /** 创建生物 */
    fun save(command: SaveCreatureInput): CreatureView

    /** 更新生物 */
    fun update(command: UpdateCreatureInput): CreatureView

    /** 删除指定 ID 的生物 */
    fun removeById(id: Long)
}
