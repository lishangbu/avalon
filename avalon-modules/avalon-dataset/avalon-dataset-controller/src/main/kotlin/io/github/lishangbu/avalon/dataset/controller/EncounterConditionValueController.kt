package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.repository.EncounterConditionValueRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
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
    private val encounterConditionValueRepository: EncounterConditionValueRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveEncounterConditionValueInput,
    ): EncounterConditionValueView = EncounterConditionValueView(encounterConditionValueRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateEncounterConditionValueInput,
    ): EncounterConditionValueView = EncounterConditionValueView(encounterConditionValueRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        encounterConditionValueRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listEncounterConditionValues(
        @ModelAttribute specification: EncounterConditionValueSpecification,
    ): List<EncounterConditionValueView> = encounterConditionValueRepository.listViews(specification)
}
