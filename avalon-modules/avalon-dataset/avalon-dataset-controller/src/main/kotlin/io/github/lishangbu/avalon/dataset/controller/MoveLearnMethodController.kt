package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.service.MoveLearnMethodService
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

/** 招式学习方式控制器 */
@RestController
@RequestMapping("/move-learn-method")
class MoveLearnMethodController(
    private val moveLearnMethodService: MoveLearnMethodService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveMoveLearnMethodInput,
    ): MoveLearnMethodView = moveLearnMethodService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateMoveLearnMethodInput,
    ): MoveLearnMethodView = moveLearnMethodService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        moveLearnMethodService.removeById(id)
    }

    @GetMapping("/list")
    fun listMoveLearnMethods(
        @ModelAttribute specification: MoveLearnMethodSpecification,
    ): List<MoveLearnMethodView> = moveLearnMethodService.listByCondition(specification)
}
