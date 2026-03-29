package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.repository.EvolutionTriggerRepository
import io.github.lishangbu.avalon.dataset.service.EvolutionTriggerService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 进化触发方式服务实现 */
@Service
class EvolutionTriggerServiceImpl(
    private val evolutionTriggerRepository: EvolutionTriggerRepository,
) : EvolutionTriggerService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveEvolutionTriggerInput): EvolutionTriggerView = EvolutionTriggerView(evolutionTriggerRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateEvolutionTriggerInput): EvolutionTriggerView = EvolutionTriggerView(evolutionTriggerRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        evolutionTriggerRepository.deleteById(id)
    }

    override fun listByCondition(specification: EvolutionTriggerSpecification): List<EvolutionTriggerView> = evolutionTriggerRepository.listViews(specification)
}
