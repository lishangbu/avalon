package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.service.PokemonEvolutionService
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
@RequestMapping("/pokemon-evolution")
class PokemonEvolutionController(
    private val pokemonEvolutionService: PokemonEvolutionService,
) {
    @GetMapping("/page")
    fun getPokemonEvolutionPage(
        pageable: Pageable,
        @ModelAttribute specification: PokemonEvolutionSpecification,
    ): Page<PokemonEvolutionView> = pokemonEvolutionService.getPageByCondition(specification, pageable)

    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SavePokemonEvolutionInput,
    ): PokemonEvolutionView = pokemonEvolutionService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdatePokemonEvolutionInput,
    ): PokemonEvolutionView = pokemonEvolutionService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        pokemonEvolutionService.removeById(id)
    }
}
