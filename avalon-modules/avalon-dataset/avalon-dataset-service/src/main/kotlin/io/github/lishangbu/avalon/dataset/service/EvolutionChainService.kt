package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionChainInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionChainInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

interface EvolutionChainService {
    fun getPageByCondition(
        specification: EvolutionChainSpecification,
        pageable: Pageable,
    ): Page<EvolutionChainView>

    fun save(command: SaveEvolutionChainInput): EvolutionChainView

    fun update(command: UpdateEvolutionChainInput): EvolutionChainView

    fun removeById(id: Long)

    fun listByCondition(specification: EvolutionChainSpecification): List<EvolutionChainView>
}
