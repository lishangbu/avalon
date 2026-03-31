package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.repository.PokemonHabitatRepository
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

/** 宝可梦栖息地控制器 */
@RestController
@RequestMapping("/pokemon-habitat")
class PokemonHabitatController(
    private val pokemonHabitatRepository: PokemonHabitatRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SavePokemonHabitatInput,
    ): PokemonHabitatView = PokemonHabitatView(pokemonHabitatRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdatePokemonHabitatInput,
    ): PokemonHabitatView = PokemonHabitatView(pokemonHabitatRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        pokemonHabitatRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listPokemonHabitats(
        @ModelAttribute specification: PokemonHabitatSpecification,
    ): List<PokemonHabitatView> = pokemonHabitatRepository.listViews(specification)
}
