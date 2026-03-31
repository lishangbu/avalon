package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EvolutionChain
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionChainInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionChainInput
import io.github.lishangbu.avalon.dataset.repository.EvolutionChainRepository
import io.github.lishangbu.avalon.dataset.service.EvolutionChainService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class EvolutionChainServiceImpl(
    private val evolutionChainRepository: EvolutionChainRepository,
) : EvolutionChainService {
    override fun getPageByCondition(
        specification: EvolutionChainSpecification,
        pageable: Pageable,
    ): Page<EvolutionChainView> = evolutionChainRepository.pageViews(specification, pageable)

    override fun save(command: SaveEvolutionChainInput): EvolutionChainView = evolutionChainRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdateEvolutionChainInput): EvolutionChainView = evolutionChainRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    override fun removeById(id: Long) {
        evolutionChainRepository.deleteById(id)
    }

    override fun listByCondition(specification: EvolutionChainSpecification): List<EvolutionChainView> = evolutionChainRepository.listViews(specification)

    private fun reloadView(evolutionChain: EvolutionChain): EvolutionChainView = requireNotNull(evolutionChainRepository.loadViewById(evolutionChain.id)) { "未找到 ID=${evolutionChain.id} 对应的进化链" }
}
