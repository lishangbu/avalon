package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationAreaInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationAreaInput
import io.github.lishangbu.avalon.dataset.service.LocationAreaService
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
@RequestMapping("/location-area")
class LocationAreaController(
    private val locationAreaService: LocationAreaService,
) {
    @GetMapping("/page")
    fun getLocationAreaPage(
        pageable: Pageable,
        @ModelAttribute specification: LocationAreaSpecification,
    ): Page<LocationAreaView> = locationAreaService.getPageByCondition(specification, pageable)

    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveLocationAreaInput,
    ): LocationAreaView = locationAreaService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateLocationAreaInput,
    ): LocationAreaView = locationAreaService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        locationAreaService.removeById(id)
    }
}
