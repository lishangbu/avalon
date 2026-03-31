package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureShapeInput
import io.github.lishangbu.avalon.dataset.service.CreatureShapeService
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

/** 生物形状控制器 */
@RestController
@RequestMapping("/creature-shapes")
class CreatureShapeController(
    private val creatureShapeService: CreatureShapeService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveCreatureShapeInput,
    ): CreatureShapeView = creatureShapeService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateCreatureShapeInput,
    ): CreatureShapeView = creatureShapeService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        creatureShapeService.removeById(id)
    }

    @GetMapping("/list")
    fun listCreatureShapes(
        @ModelAttribute specification: CreatureShapeSpecification,
    ): List<CreatureShapeView> = creatureShapeService.listByCondition(specification)
}
