package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.LocationSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationInput
import io.github.lishangbu.avalon.dataset.service.LocationService
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
@RequestMapping("/location")
class LocationController(
    private val locationService: LocationService,
) {
    @GetMapping("/page")
    fun getLocationPage(
        pageable: Pageable,
        @ModelAttribute specification: LocationSpecification,
    ): Page<LocationView> = locationService.getPageByCondition(specification, pageable)

    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveLocationInput,
    ): LocationView = locationService.save(command)

    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateLocationInput,
    ): LocationView = locationService.update(command)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        locationService.removeById(id)
    }

    @GetMapping("/list")
    fun listLocations(
        @ModelAttribute specification: LocationSpecification,
    ): List<LocationView> = locationService.listByCondition(specification)
}
