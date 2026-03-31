package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionInput
import io.github.lishangbu.avalon.dataset.service.EncounterConditionService
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

/** 遭遇条件控制器 */
@RestController
@RequestMapping("/encounter-condition")
class EncounterConditionController(
    private val encounterConditionService: EncounterConditionService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveEncounterConditionInput,
    ): EncounterConditionView = encounterConditionService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateEncounterConditionInput,
    ): EncounterConditionView = encounterConditionService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        encounterConditionService.removeById(id)
    }

    @GetMapping("/list")
    fun listEncounterConditions(
        @ModelAttribute specification: EncounterConditionSpecification,
    ): List<EncounterConditionView> = encounterConditionService.listByCondition(specification)
}
