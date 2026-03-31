package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureColorInput
import io.github.lishangbu.avalon.dataset.repository.CreatureColorRepository
import io.github.lishangbu.avalon.dataset.service.CreatureColorService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 生物颜色服务实现 */
@Service
class CreatureColorServiceImpl(
    private val creatureColorRepository: CreatureColorRepository,
) : CreatureColorService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveCreatureColorInput): CreatureColorView = CreatureColorView(creatureColorRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateCreatureColorInput): CreatureColorView = CreatureColorView(creatureColorRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        creatureColorRepository.deleteById(id)
    }

    override fun listByCondition(specification: CreatureColorSpecification): List<CreatureColorView> = creatureColorRepository.listViews(specification)
}
