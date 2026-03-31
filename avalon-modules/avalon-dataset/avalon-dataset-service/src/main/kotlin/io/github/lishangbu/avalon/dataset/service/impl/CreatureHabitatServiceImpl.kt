package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.repository.CreatureHabitatRepository
import io.github.lishangbu.avalon.dataset.service.CreatureHabitatService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 生物栖息地服务实现 */
@Service
class CreatureHabitatServiceImpl(
    private val creatureHabitatRepository: CreatureHabitatRepository,
) : CreatureHabitatService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveCreatureHabitatInput): CreatureHabitatView = CreatureHabitatView(creatureHabitatRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateCreatureHabitatInput): CreatureHabitatView = CreatureHabitatView(creatureHabitatRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        creatureHabitatRepository.deleteById(id)
    }

    override fun listByCondition(specification: CreatureHabitatSpecification): List<CreatureHabitatView> = creatureHabitatRepository.listViews(specification)
}
