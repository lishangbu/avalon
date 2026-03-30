package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonShapeInput
import io.github.lishangbu.avalon.dataset.service.PokemonShapeService
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

/** 宝可梦形状控制器 */
@RestController
@RequestMapping("/pokemon-shape")
class PokemonShapeController(
    private val pokemonShapeService: PokemonShapeService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SavePokemonShapeInput,
    ): PokemonShapeView = pokemonShapeService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdatePokemonShapeInput,
    ): PokemonShapeView = pokemonShapeService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        pokemonShapeService.removeById(id)
    }

    @GetMapping("/list")
    fun listPokemonShapes(
        @ModelAttribute specification: PokemonShapeSpecification,
    ): List<PokemonShapeView> = pokemonShapeService.listByCondition(specification)
}
