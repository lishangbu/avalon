package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveAilmentInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveAilmentInput
import io.github.lishangbu.avalon.dataset.repository.MoveAilmentRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 招式异常控制器 */
@RestController
@RequestMapping("/move-ailment")
class MoveAilmentController(
    private val moveAilmentRepository: MoveAilmentRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveAilmentInput,
    ): MoveAilmentView = MoveAilmentView(moveAilmentRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveAilmentInput,
    ): MoveAilmentView = MoveAilmentView(moveAilmentRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveAilmentRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listMoveAilments(
        @ModelAttribute specification: MoveAilmentSpecification,
    ): List<MoveAilmentView> = moveAilmentRepository.listViews(specification)
}
