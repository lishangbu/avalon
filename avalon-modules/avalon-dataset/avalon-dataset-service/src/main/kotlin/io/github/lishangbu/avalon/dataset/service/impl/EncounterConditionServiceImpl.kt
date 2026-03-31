package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionInput
import io.github.lishangbu.avalon.dataset.repository.EncounterConditionRepository
import io.github.lishangbu.avalon.dataset.repository.EncounterConditionValueRepository
import io.github.lishangbu.avalon.dataset.service.EncounterConditionService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 遭遇条件服务实现 */
@Service
class EncounterConditionServiceImpl(
    private val encounterConditionRepository: EncounterConditionRepository,
    private val encounterConditionValueRepository: EncounterConditionValueRepository,
) : EncounterConditionService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveEncounterConditionInput): EncounterConditionView = EncounterConditionView(encounterConditionRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateEncounterConditionInput): EncounterConditionView = EncounterConditionView(encounterConditionRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        if (encounterConditionValueRepository.existsByEncounterConditionId(id)) {
            throw IllegalStateException("当前遭遇条件下仍存在遭遇条件值，请先删除相关遭遇条件值后再删除当前遭遇条件")
        }
        encounterConditionRepository.deleteById(id)
    }

    override fun listByCondition(specification: EncounterConditionSpecification): List<EncounterConditionView> = encounterConditionRepository.listViews(specification)
}
