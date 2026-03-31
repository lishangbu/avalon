package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationAreaInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationAreaInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

interface LocationAreaService {
    fun getPageByCondition(
        specification: LocationAreaSpecification,
        pageable: Pageable,
    ): Page<LocationAreaView>

    fun save(command: SaveLocationAreaInput): LocationAreaView

    fun update(command: UpdateLocationAreaInput): LocationAreaView

    fun removeById(id: Long)
}
