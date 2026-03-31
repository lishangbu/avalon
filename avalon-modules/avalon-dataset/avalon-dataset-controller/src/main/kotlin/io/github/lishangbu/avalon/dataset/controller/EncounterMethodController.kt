package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterMethodInput
import io.github.lishangbu.avalon.dataset.service.EncounterMethodService
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

/** 遭遇方式控制器 */
@RestController
@RequestMapping("/encounter-method")
class EncounterMethodController(
    private val encounterMethodService: EncounterMethodService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveEncounterMethodInput,
    ): EncounterMethodView = encounterMethodService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateEncounterMethodInput,
    ): EncounterMethodView = encounterMethodService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        encounterMethodService.removeById(id)
    }

    @GetMapping("/list")
    fun listEncounterMethods(
        @ModelAttribute specification: EncounterMethodSpecification,
    ): List<EncounterMethodView> = encounterMethodService.listByCondition(specification)
}
