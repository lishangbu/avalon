package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveAilmentInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveAilmentInput
import io.github.lishangbu.avalon.dataset.repository.MoveAilmentRepository
import io.github.lishangbu.avalon.dataset.service.MoveAilmentService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 招式异常服务实现 */
@Service
class MoveAilmentServiceImpl(
    private val moveAilmentRepository: MoveAilmentRepository,
) : MoveAilmentService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveMoveAilmentInput): MoveAilmentView = MoveAilmentView(moveAilmentRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateMoveAilmentInput): MoveAilmentView = MoveAilmentView(moveAilmentRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        moveAilmentRepository.deleteById(id)
    }

    override fun listByCondition(specification: MoveAilmentSpecification): List<MoveAilmentView> = moveAilmentRepository.listViews(specification)
}
