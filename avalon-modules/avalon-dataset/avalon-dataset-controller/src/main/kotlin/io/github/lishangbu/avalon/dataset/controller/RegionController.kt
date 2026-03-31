package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.RegionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.RegionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveRegionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateRegionInput
import io.github.lishangbu.avalon.dataset.service.RegionService
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

@RestController
@RequestMapping("/region")
class RegionController(
    private val regionService: RegionService,
) {
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveRegionInput,
    ): RegionView = regionService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateRegionInput,
    ): RegionView = regionService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        regionService.removeById(id)
    }

    @GetMapping("/list")
    fun listRegions(
        @ModelAttribute specification: RegionSpecification,
    ): List<RegionView> = regionService.listByCondition(specification)
}
