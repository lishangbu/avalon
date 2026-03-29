package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Move
import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveInput
import io.github.lishangbu.avalon.dataset.repository.MoveRepository
import io.github.lishangbu.avalon.dataset.service.MoveService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 招式应用服务实现 */
@Service
class MoveServiceImpl(
    private val moveRepository: MoveRepository,
) : MoveService {
    override fun getPageByCondition(
        specification: MoveSpecification,
        pageable: Pageable,
    ): Page<MoveView> = moveRepository.pageViews(specification, pageable)

    override fun save(command: SaveMoveInput): MoveView = moveRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdateMoveInput): MoveView = moveRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    override fun removeById(id: Long) {
        moveRepository.deleteById(id)
    }

    private fun reloadView(move: Move): MoveView = requireNotNull(moveRepository.loadViewById(move.id)) { "未找到 ID=${move.id} 对应的招式" }
}
