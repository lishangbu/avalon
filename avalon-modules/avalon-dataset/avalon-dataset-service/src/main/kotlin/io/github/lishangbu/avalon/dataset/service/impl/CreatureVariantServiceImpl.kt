package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.CreatureVariant
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureVariantInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureVariantInput
import io.github.lishangbu.avalon.dataset.repository.CreatureVariantRepository
import io.github.lishangbu.avalon.dataset.service.CreatureVariantService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class CreatureVariantServiceImpl(
    private val creatureVariantRepository: CreatureVariantRepository,
) : CreatureVariantService {
    override fun getPageByCondition(
        specification: CreatureVariantSpecification,
        pageable: Pageable,
    ): Page<CreatureVariantView> = creatureVariantRepository.pageViews(specification, pageable)

    override fun save(command: SaveCreatureVariantInput): CreatureVariantView = creatureVariantRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdateCreatureVariantInput): CreatureVariantView = creatureVariantRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    override fun removeById(id: Long) {
        creatureVariantRepository.deleteById(id)
    }

    private fun reloadView(creatureVariant: CreatureVariant): CreatureVariantView = requireNotNull(creatureVariantRepository.loadViewById(creatureVariant.id)) { "未找到 ID=${creatureVariant.id} 对应的生物变体" }
}
