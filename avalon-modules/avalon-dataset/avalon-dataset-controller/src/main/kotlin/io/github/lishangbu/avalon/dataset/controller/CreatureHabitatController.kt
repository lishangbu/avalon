package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.service.CreatureHabitatService
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

/** 生物栖息地控制器 */
@RestController
@RequestMapping("/creature-habitats")
class CreatureHabitatController(
    private val creatureHabitatService: CreatureHabitatService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveCreatureHabitatInput,
    ): CreatureHabitatView = creatureHabitatService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateCreatureHabitatInput,
    ): CreatureHabitatView = creatureHabitatService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        creatureHabitatService.removeById(id)
    }

    @GetMapping("/list")
    fun listCreatureHabitats(
        @ModelAttribute specification: CreatureHabitatSpecification,
    ): List<CreatureHabitatView> = creatureHabitatService.listByCondition(specification)
}
