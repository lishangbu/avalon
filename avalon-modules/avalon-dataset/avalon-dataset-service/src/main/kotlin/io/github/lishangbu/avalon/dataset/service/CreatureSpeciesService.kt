package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureSpeciesInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 生物种族应用服务 */
interface CreatureSpeciesService {
    /** 按筛选条件分页查询生物种族 */
    fun getPageByCondition(
        specification: CreatureSpeciesSpecification,
        pageable: Pageable,
    ): Page<CreatureSpeciesView>

    /** 创建生物种族 */
    fun save(command: SaveCreatureSpeciesInput): CreatureSpeciesView

    /** 更新生物种族 */
    fun update(command: UpdateCreatureSpeciesInput): CreatureSpeciesView

    /** 删除指定 ID 的生物种族 */
    fun removeById(id: Long)
}
