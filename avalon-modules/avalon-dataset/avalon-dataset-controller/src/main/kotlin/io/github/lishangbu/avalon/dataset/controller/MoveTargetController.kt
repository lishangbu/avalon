package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveTargetInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveTargetInput
import io.github.lishangbu.avalon.dataset.service.MoveTargetService
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

/** 招式目标控制器 */
@RestController
@RequestMapping("/move-target")
class MoveTargetController(
    private val moveTargetService: MoveTargetService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveTargetInput,
    ): MoveTargetView = moveTargetService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveTargetInput,
    ): MoveTargetView = moveTargetService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveTargetService.removeById(id)
    }

    @GetMapping("/list")
    fun listMoveTargets(
        @ModelAttribute specification: MoveTargetSpecification,
    ): List<MoveTargetView> = moveTargetService.listByCondition(specification)
}
