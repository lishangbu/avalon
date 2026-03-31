package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.repository.CreatureSpeciesRepository
import io.github.lishangbu.avalon.dataset.service.CreatureSpeciesService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 生物种族应用服务实现 */
@Service
class CreatureSpeciesServiceImpl(
    private val creatureSpeciesRepository: CreatureSpeciesRepository,
) : CreatureSpeciesService {
    /** 按筛选条件分页查询生物种族 */
    override fun getPageByCondition(
        specification: CreatureSpeciesSpecification,
        pageable: Pageable,
    ): Page<CreatureSpeciesView> = creatureSpeciesRepository.pageViews(specification, pageable)

    /** 创建生物种族 */
    override fun save(command: SaveCreatureSpeciesInput): CreatureSpeciesView = creatureSpeciesRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新生物种族 */
    override fun update(command: UpdateCreatureSpeciesInput): CreatureSpeciesView = creatureSpeciesRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    /** 删除指定 ID 的生物种族 */
    override fun removeById(id: Long) {
        creatureSpeciesRepository.deleteById(id)
    }

    private fun reloadView(creatureSpecies: CreatureSpecies): CreatureSpeciesView = requireNotNull(creatureSpeciesRepository.loadViewById(creatureSpecies.id)) { "未找到 ID=${creatureSpecies.id} 对应的生物种族" }
}
