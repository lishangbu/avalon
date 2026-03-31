package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterMethodInput
import io.github.lishangbu.avalon.dataset.repository.EncounterMethodRepository
import io.github.lishangbu.avalon.dataset.service.EncounterMethodService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 遭遇方式服务实现 */
@Service
class EncounterMethodServiceImpl(
    private val encounterMethodRepository: EncounterMethodRepository,
) : EncounterMethodService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveEncounterMethodInput): EncounterMethodView = EncounterMethodView(encounterMethodRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateEncounterMethodInput): EncounterMethodView = EncounterMethodView(encounterMethodRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        encounterMethodRepository.deleteById(id)
    }

    override fun listByCondition(specification: EncounterMethodSpecification): List<EncounterMethodView> = encounterMethodRepository.listViews(specification)
}
