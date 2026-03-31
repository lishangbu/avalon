package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.repository.MoveLearnMethodRepository
import io.github.lishangbu.avalon.dataset.service.MoveLearnMethodService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 招式学习方式服务实现 */
@Service
class MoveLearnMethodServiceImpl(
    private val moveLearnMethodRepository: MoveLearnMethodRepository,
) : MoveLearnMethodService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveMoveLearnMethodInput): MoveLearnMethodView = MoveLearnMethodView(moveLearnMethodRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateMoveLearnMethodInput): MoveLearnMethodView = MoveLearnMethodView(moveLearnMethodRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        moveLearnMethodRepository.deleteById(id)
    }

    override fun listByCondition(specification: MoveLearnMethodSpecification): List<MoveLearnMethodView> = moveLearnMethodRepository.listViews(specification)
}
