package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureShapeInput
import io.github.lishangbu.avalon.dataset.repository.CreatureShapeRepository
import io.github.lishangbu.avalon.dataset.service.CreatureShapeService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 生物形状服务实现 */
@Service
class CreatureShapeServiceImpl(
    private val creatureShapeRepository: CreatureShapeRepository,
) : CreatureShapeService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveCreatureShapeInput): CreatureShapeView = CreatureShapeView(creatureShapeRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateCreatureShapeInput): CreatureShapeView = CreatureShapeView(creatureShapeRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        creatureShapeRepository.deleteById(id)
    }

    override fun listByCondition(specification: CreatureShapeSpecification): List<CreatureShapeView> = creatureShapeRepository.listViews(specification)
}
