package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionChainView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionChainInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionChainInput
import io.github.lishangbu.avalon.dataset.service.EvolutionChainService
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
@RequestMapping("/evolution-chain")
class EvolutionChainController(
    private val evolutionChainService: EvolutionChainService,
) {
    @GetMapping("/page")
    fun getEvolutionChainPage(
        pageable: Pageable,
        @ModelAttribute specification: EvolutionChainSpecification,
    ): Page<EvolutionChainView> = evolutionChainService.getPageByCondition(specification, pageable)

    @GetMapping("/list")
    fun listEvolutionChains(
        @ModelAttribute specification: EvolutionChainSpecification,
    ): List<EvolutionChainView> = evolutionChainService.listByCondition(specification)

    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveEvolutionChainInput,
    ): EvolutionChainView = evolutionChainService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateEvolutionChainInput,
    ): EvolutionChainView = evolutionChainService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        evolutionChainService.removeById(id)
    }
}
