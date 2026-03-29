package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionInput
import io.github.lishangbu.avalon.dataset.repository.EncounterConditionRepository
import io.github.lishangbu.avalon.dataset.service.EncounterConditionService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 遭遇条件服务实现 */
@Service
class EncounterConditionServiceImpl(
    private val encounterConditionRepository: EncounterConditionRepository,
) : EncounterConditionService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveEncounterConditionInput): EncounterConditionView = EncounterConditionView(encounterConditionRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateEncounterConditionInput): EncounterConditionView = EncounterConditionView(encounterConditionRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        encounterConditionRepository.deleteById(id)
    }

    override fun listByCondition(specification: EncounterConditionSpecification): List<EncounterConditionView> = encounterConditionRepository.listViews(specification)
}
