package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.LocationArea
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationAreaInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationAreaInput
import io.github.lishangbu.avalon.dataset.repository.LocationAreaRepository
import io.github.lishangbu.avalon.dataset.service.LocationAreaService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class LocationAreaServiceImpl(
    private val locationAreaRepository: LocationAreaRepository,
) : LocationAreaService {
    override fun getPageByCondition(
        specification: LocationAreaSpecification,
        pageable: Pageable,
    ): Page<LocationAreaView> = locationAreaRepository.pageViews(specification, pageable)

    override fun save(command: SaveLocationAreaInput): LocationAreaView = locationAreaRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdateLocationAreaInput): LocationAreaView = locationAreaRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    override fun removeById(id: Long) {
        locationAreaRepository.deleteById(id)
    }

    private fun reloadView(locationArea: LocationArea): LocationAreaView = requireNotNull(locationAreaRepository.loadViewById(locationArea.id)) { "未找到 ID=${locationArea.id} 对应的地点区域" }
}
