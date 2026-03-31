package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.CreatureEvolution
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.repository.CreatureEvolutionRepository
import io.github.lishangbu.avalon.dataset.service.CreatureEvolutionService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class CreatureEvolutionServiceImpl(
    private val creatureEvolutionRepository: CreatureEvolutionRepository,
) : CreatureEvolutionService {
    override fun getPageByCondition(
        specification: CreatureEvolutionSpecification,
        pageable: Pageable,
    ): Page<CreatureEvolutionView> = creatureEvolutionRepository.pageViews(specification, pageable)

    override fun save(command: SaveCreatureEvolutionInput): CreatureEvolutionView = creatureEvolutionRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdateCreatureEvolutionInput): CreatureEvolutionView = creatureEvolutionRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    override fun removeById(id: Long) {
        creatureEvolutionRepository.deleteById(id)
    }

    private fun reloadView(creatureEvolution: CreatureEvolution): CreatureEvolutionView = requireNotNull(creatureEvolutionRepository.loadViewById(creatureEvolution.id)) { "未找到 ID=${creatureEvolution.id} 对应的生物进化条件" }
}
