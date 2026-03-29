package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.AbilityView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveAbilityInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateAbilityInput
import io.github.lishangbu.avalon.dataset.repository.AbilityRepository
import io.github.lishangbu.avalon.dataset.service.AbilityService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 特性服务实现 */
@Service
class AbilityServiceImpl(
    private val abilityRepository: AbilityRepository,
) : AbilityService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveAbilityInput): AbilityView = AbilityView(abilityRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateAbilityInput): AbilityView = AbilityView(abilityRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        abilityRepository.deleteById(id)
    }

    override fun listByCondition(specification: AbilitySpecification): List<AbilityView> = abilityRepository.listViews(specification)
}
