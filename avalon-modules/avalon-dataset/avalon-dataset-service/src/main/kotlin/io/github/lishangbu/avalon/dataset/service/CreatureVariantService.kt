package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureVariantInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureVariantInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

interface CreatureVariantService {
    fun getPageByCondition(
        specification: CreatureVariantSpecification,
        pageable: Pageable,
    ): Page<CreatureVariantView>

    fun save(command: SaveCreatureVariantInput): CreatureVariantView

    fun update(command: UpdateCreatureVariantInput): CreatureVariantView

    fun removeById(id: Long)
}
