package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveTargetInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveTargetInput
import io.github.lishangbu.avalon.dataset.repository.MoveTargetRepository
import io.github.lishangbu.avalon.dataset.service.MoveTargetService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 招式目标服务实现 */
@Service
class MoveTargetServiceImpl(
    private val moveTargetRepository: MoveTargetRepository,
) : MoveTargetService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveMoveTargetInput): MoveTargetView = MoveTargetView(moveTargetRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateMoveTargetInput): MoveTargetView = MoveTargetView(moveTargetRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        moveTargetRepository.deleteById(id)
    }

    override fun listByCondition(specification: MoveTargetSpecification): List<MoveTargetView> = moveTargetRepository.listViews(specification)
}
