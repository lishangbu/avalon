package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.service.EvolutionTriggerService
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

/** 进化触发方式控制器 */
@RestController
@RequestMapping("/evolution-trigger")
class EvolutionTriggerController(
    private val evolutionTriggerService: EvolutionTriggerService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveEvolutionTriggerInput,
    ): EvolutionTriggerView = evolutionTriggerService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateEvolutionTriggerInput,
    ): EvolutionTriggerView = evolutionTriggerService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        evolutionTriggerService.removeById(id)
    }

    @GetMapping("/list")
    fun listEvolutionTriggers(
        @ModelAttribute specification: EvolutionTriggerSpecification,
    ): List<EvolutionTriggerView> = evolutionTriggerService.listByCondition(specification)
}
