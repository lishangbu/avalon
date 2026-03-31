package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureColorInput
import io.github.lishangbu.avalon.dataset.service.CreatureColorService
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

/** 生物颜色控制器 */
@RestController
@RequestMapping("/creature-colors")
class CreatureColorController(
    private val creatureColorService: CreatureColorService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveCreatureColorInput,
    ): CreatureColorView = creatureColorService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateCreatureColorInput,
    ): CreatureColorView = creatureColorService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        creatureColorService.removeById(id)
    }

    @GetMapping("/list")
    fun listCreatureColors(
        @ModelAttribute specification: CreatureColorSpecification,
    ): List<CreatureColorView> = creatureColorService.listByCondition(specification)
}
