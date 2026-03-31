package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureVariantInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureVariantInput
import io.github.lishangbu.avalon.dataset.service.CreatureVariantService
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
@RequestMapping("/creature-variants")
class CreatureVariantController(
    private val creatureVariantService: CreatureVariantService,
) {
    @GetMapping("/page")
    fun getCreatureVariantPage(
        pageable: Pageable,
        @ModelAttribute specification: CreatureVariantSpecification,
    ): Page<CreatureVariantView> = creatureVariantService.getPageByCondition(specification, pageable)

    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveCreatureVariantInput,
    ): CreatureVariantView = creatureVariantService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateCreatureVariantInput,
    ): CreatureVariantView = creatureVariantService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        creatureVariantService.removeById(id)
    }
}
