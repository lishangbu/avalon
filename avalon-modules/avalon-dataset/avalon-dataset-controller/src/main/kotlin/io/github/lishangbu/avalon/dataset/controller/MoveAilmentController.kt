package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveAilmentInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveAilmentInput
import io.github.lishangbu.avalon.dataset.service.MoveAilmentService
import jakarta.validation.Valid
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
    private val moveAilmentService: MoveAilmentService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveAilmentInput,
    ): MoveAilmentView = moveAilmentService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveAilmentInput,
    ): MoveAilmentView = moveAilmentService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveAilmentService.removeById(id)
    }

    @GetMapping("/list")
    fun listMoveAilments(
        @ModelAttribute specification: MoveAilmentSpecification,
    ): List<MoveAilmentView> = moveAilmentService.listByCondition(specification)
}
