package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.service.PokemonHabitatService
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
    private val pokemonHabitatService: PokemonHabitatService,
) {
    @PostMapping
    fun save(
        @RequestBody command: SavePokemonHabitatInput,
    ): PokemonHabitatView = pokemonHabitatService.save(command)

    @PutMapping
    fun update(
        @RequestBody command: UpdatePokemonHabitatInput,
    ): PokemonHabitatView = pokemonHabitatService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        pokemonHabitatService.removeById(id)
    }

    @GetMapping("/list")
    fun listPokemonHabitats(
        @ModelAttribute specification: PokemonHabitatSpecification,
    ): List<PokemonHabitatView> = pokemonHabitatService.listByCondition(specification)
}
