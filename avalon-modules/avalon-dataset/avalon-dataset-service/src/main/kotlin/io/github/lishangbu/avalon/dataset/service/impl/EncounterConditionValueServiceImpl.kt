package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EncounterConditionValue
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.repository.EncounterConditionValueRepository
import io.github.lishangbu.avalon.dataset.repository.LocationAreaEncounterConditionValueRepository
import io.github.lishangbu.avalon.dataset.service.EncounterConditionValueService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 遭遇条件值服务实现 */
@Service
class EncounterConditionValueServiceImpl(
    private val encounterConditionValueRepository: EncounterConditionValueRepository,
    private val locationAreaEncounterConditionValueRepository: LocationAreaEncounterConditionValueRepository,
) : EncounterConditionValueService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveEncounterConditionValueInput): EncounterConditionValueView = encounterConditionValueRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateEncounterConditionValueInput): EncounterConditionValueView = encounterConditionValueRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        if (locationAreaEncounterConditionValueRepository.existsByEncounterConditionValueId(id)) {
            throw IllegalStateException("当前遭遇条件值已被地点区域遭遇引用，请先删除相关遭遇引用后再删除当前遭遇条件值")
        }
        encounterConditionValueRepository.deleteById(id)
    }

    override fun listByCondition(specification: EncounterConditionValueSpecification): List<EncounterConditionValueView> = encounterConditionValueRepository.listViews(specification)

    private fun reloadView(encounterConditionValue: EncounterConditionValue): EncounterConditionValueView =
        requireNotNull(encounterConditionValueRepository.loadViewById(encounterConditionValue.id)) {
            "未找到 ID=${encounterConditionValue.id} 对应的遭遇条件值"
        }
}
