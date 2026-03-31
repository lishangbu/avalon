package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.RegionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.RegionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveRegionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateRegionInput

interface RegionService {
    fun save(command: SaveRegionInput): RegionView

    fun update(command: UpdateRegionInput): RegionView

    fun removeById(id: Long)

    fun listByCondition(specification: RegionSpecification): List<RegionView>
}
