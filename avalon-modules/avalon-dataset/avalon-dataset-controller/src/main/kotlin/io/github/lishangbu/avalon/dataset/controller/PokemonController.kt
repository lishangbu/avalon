package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonInput
import io.github.lishangbu.avalon.dataset.service.PokemonService
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

/** 宝可梦管理控制器 */
@RestController
@RequestMapping("/pokemon")
class PokemonController(
    private val pokemonService: PokemonService,
) {
    /** 按筛选条件分页查询宝可梦 */
    @GetMapping("/page")
    fun getPokemonPage(
        pageable: Pageable,
        @ModelAttribute specification: PokemonSpecification,
    ): Page<PokemonView> = pokemonService.getPageByCondition(specification, pageable)

    /** 创建宝可梦 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SavePokemonInput,
    ): PokemonView = pokemonService.save(command)

    /** 更新宝可梦 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdatePokemonInput,
    ): PokemonView = pokemonService.update(command)

    /** 删除指定 ID 的宝可梦 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        pokemonService.removeById(id)
    }
}
