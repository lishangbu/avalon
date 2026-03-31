package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonColorInput
import io.github.lishangbu.avalon.dataset.repository.PokemonColorRepository
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

/** 宝可梦颜色控制器 */
@RestController
@RequestMapping("/pokemon-color")
class PokemonColorController(
    private val pokemonColorRepository: PokemonColorRepository,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SavePokemonColorInput,
    ): PokemonColorView = PokemonColorView(pokemonColorRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdatePokemonColorInput,
    ): PokemonColorView = PokemonColorView(pokemonColorRepository.save(command.toEntity(), SaveMode.UPSERT))

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        pokemonColorRepository.deleteById(id)
    }

    @GetMapping("/list")
    fun listPokemonColors(
        @ModelAttribute specification: PokemonColorSpecification,
    ): List<PokemonColorView> = pokemonColorRepository.listViews(specification)
}
