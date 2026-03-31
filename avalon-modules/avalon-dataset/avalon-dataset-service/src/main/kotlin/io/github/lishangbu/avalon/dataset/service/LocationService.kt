package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.LocationSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

interface LocationService {
    fun getPageByCondition(
        specification: LocationSpecification,
        pageable: Pageable,
    ): Page<LocationView>

    fun save(command: SaveLocationInput): LocationView

    fun update(command: UpdateLocationInput): LocationView

    fun removeById(id: Long)

    fun listByCondition(specification: LocationSpecification): List<LocationView>
}
