package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.service.PokemonSpeciesService
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

/** 宝可梦种族管理控制器 */
@RestController
@RequestMapping("/pokemon-species")
class PokemonSpeciesController(
    private val pokemonSpeciesService: PokemonSpeciesService,
) {
    /** 按筛选条件分页查询宝可梦种族 */
    @GetMapping("/page")
    fun getPokemonSpeciesPage(
        pageable: Pageable,
        @ModelAttribute specification: PokemonSpeciesSpecification,
    ): Page<PokemonSpeciesView> = pokemonSpeciesService.getPageByCondition(specification, pageable)

    /** 创建宝可梦种族 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SavePokemonSpeciesInput,
    ): PokemonSpeciesView = pokemonSpeciesService.save(command)

    /** 更新宝可梦种族 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdatePokemonSpeciesInput,
    ): PokemonSpeciesView = pokemonSpeciesService.update(command)

    /** 删除指定 ID 的宝可梦种族 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        pokemonSpeciesService.removeById(id)
    }
}
