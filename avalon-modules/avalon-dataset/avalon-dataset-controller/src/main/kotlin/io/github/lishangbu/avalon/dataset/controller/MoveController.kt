package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Move
import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveInput
import io.github.lishangbu.avalon.dataset.repository.MoveRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 招式管理控制器 */
@RestController
@RequestMapping("/move")
class MoveController(
    private val moveRepository: MoveRepository,
) {
    /** 按筛选条件分页查询招式 */
    @GetMapping("/page")
    fun getMovePage(
        pageable: Pageable,
        @ModelAttribute specification: MoveSpecification,
    ): Page<MoveView> = moveRepository.pageViews(specification, pageable)

    /** 创建招式 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveInput,
    ): MoveView = moveRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新招式 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveInput,
    ): MoveView = moveRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    /** 删除指定 ID 的招式 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveRepository.deleteById(id)
    }

    private fun reloadView(move: Move): MoveView = requireNotNull(moveRepository.loadViewById(move.id)) { "未找到 ID=${move.id} 对应的招式" }
}
