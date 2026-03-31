package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionInput
import io.github.lishangbu.avalon.dataset.repository.EncounterConditionRepository
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

/** 遭遇条件控制器 */
@RestController
@RequestMapping("/encounter-condition")
class EncounterConditionController(
    private val encounterConditionRepository: EncounterConditionRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveEncounterConditionInput,
    ): EncounterConditionView = EncounterConditionView(encounterConditionRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateEncounterConditionInput,
    ): EncounterConditionView = EncounterConditionView(encounterConditionRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        encounterConditionRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listEncounterConditions(
        @ModelAttribute specification: EncounterConditionSpecification,
    ): List<EncounterConditionView> = encounterConditionRepository.listViews(specification)
}
