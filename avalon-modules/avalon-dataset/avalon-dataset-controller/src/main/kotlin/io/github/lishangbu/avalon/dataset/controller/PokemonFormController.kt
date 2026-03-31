package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonFormInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonFormInput
import io.github.lishangbu.avalon.dataset.service.PokemonFormService
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
@RequestMapping("/pokemon-form")
class PokemonFormController(
    private val pokemonFormService: PokemonFormService,
) {
    @GetMapping("/page")
    fun getPokemonFormPage(
        pageable: Pageable,
        @ModelAttribute specification: PokemonFormSpecification,
    ): Page<PokemonFormView> = pokemonFormService.getPageByCondition(specification, pageable)

    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SavePokemonFormInput,
    ): PokemonFormView = pokemonFormService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdatePokemonFormInput,
    ): PokemonFormView = pokemonFormService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        pokemonFormService.removeById(id)
    }
}
