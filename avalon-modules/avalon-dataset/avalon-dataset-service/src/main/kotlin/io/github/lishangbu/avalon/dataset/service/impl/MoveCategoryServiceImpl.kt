package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveCategoryInput
import io.github.lishangbu.avalon.dataset.repository.MoveCategoryRepository
import io.github.lishangbu.avalon.dataset.service.MoveCategoryService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 招式类别服务实现 */
@Service
class MoveCategoryServiceImpl(
    private val moveCategoryRepository: MoveCategoryRepository,
) : MoveCategoryService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveMoveCategoryInput): MoveCategoryView = MoveCategoryView(moveCategoryRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateMoveCategoryInput): MoveCategoryView = MoveCategoryView(moveCategoryRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        moveCategoryRepository.deleteById(id)
    }

    override fun listByCondition(specification: MoveCategorySpecification): List<MoveCategoryView> = moveCategoryRepository.listViews(specification)
}
