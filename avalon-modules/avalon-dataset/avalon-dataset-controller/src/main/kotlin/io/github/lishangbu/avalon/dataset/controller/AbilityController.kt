package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.AbilityView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveAbilityInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateAbilityInput
import io.github.lishangbu.avalon.dataset.service.AbilityService
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

/** 特性控制器 */
@RestController
@RequestMapping("/ability")
class AbilityController(
    private val abilityService: AbilityService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveAbilityInput,
    ): AbilityView = abilityService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateAbilityInput,
    ): AbilityView = abilityService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        abilityService.removeById(id)
    }

    @GetMapping("/list")
    fun listAbilities(
        @ModelAttribute specification: AbilitySpecification,
    ): List<AbilityView> = abilityService.listByCondition(specification)
}
