package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.service.CreatureEvolutionService
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
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

@RestController
@RequestMapping("/creature-evolutions")
class CreatureEvolutionController(
    private val creatureEvolutionService: CreatureEvolutionService,
) {
    @GetMapping("/page")
    fun getCreatureEvolutionPage(
        pageable: Pageable,
        @ModelAttribute specification: CreatureEvolutionSpecification,
    ): Page<CreatureEvolutionView> = creatureEvolutionService.getPageByCondition(specification, pageable)

    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveCreatureEvolutionInput,
    ): CreatureEvolutionView = creatureEvolutionService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateCreatureEvolutionInput,
    ): CreatureEvolutionView = creatureEvolutionService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        creatureEvolutionService.removeById(id)
    }
}
