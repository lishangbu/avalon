package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureEvolutionInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

interface CreatureEvolutionService {
    fun getPageByCondition(
        specification: CreatureEvolutionSpecification,
        pageable: Pageable,
    ): Page<CreatureEvolutionView>

    fun save(command: SaveCreatureEvolutionInput): CreatureEvolutionView

    fun update(command: UpdateCreatureEvolutionInput): CreatureEvolutionView

    fun removeById(id: Long)
}
