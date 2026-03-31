package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Location
import io.github.lishangbu.avalon.dataset.entity.dto.LocationSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationInput
import io.github.lishangbu.avalon.dataset.repository.LocationRepository
import io.github.lishangbu.avalon.dataset.service.LocationService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class LocationServiceImpl(
    private val locationRepository: LocationRepository,
) : LocationService {
    override fun getPageByCondition(
        specification: LocationSpecification,
        pageable: Pageable,
    ): Page<LocationView> = locationRepository.pageViews(specification, pageable)

    override fun save(command: SaveLocationInput): LocationView = locationRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdateLocationInput): LocationView = locationRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    override fun removeById(id: Long) {
        locationRepository.deleteById(id)
    }

    override fun listByCondition(specification: LocationSpecification): List<LocationView> = locationRepository.listViews(specification)

    private fun reloadView(location: Location): LocationView = requireNotNull(locationRepository.loadViewById(location.id)) { "未找到 ID=${location.id} 对应的地点" }
}
