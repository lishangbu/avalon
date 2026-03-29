package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.service.EncounterConditionValueService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 遭遇条件值控制器 */
@RestController
@RequestMapping("/encounter-condition-value")
class EncounterConditionValueController(
    private val encounterConditionValueService: EncounterConditionValueService,
) {
    @PostMapping
    fun save(
        @RequestBody command: SaveEncounterConditionValueInput,
    ): EncounterConditionValueView = encounterConditionValueService.save(command)

    @PutMapping
    fun update(
        @RequestBody command: UpdateEncounterConditionValueInput,
    ): EncounterConditionValueView = encounterConditionValueService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        encounterConditionValueService.removeById(id)
    }

    @GetMapping("/list")
    fun listEncounterConditionValues(
        @ModelAttribute specification: EncounterConditionValueSpecification,
    ): List<EncounterConditionValueView> = encounterConditionValueService.listByCondition(specification)
}
