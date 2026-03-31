package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Region
import io.github.lishangbu.avalon.dataset.entity.dto.RegionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.RegionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveRegionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateRegionInput
import io.github.lishangbu.avalon.dataset.repository.RegionRepository
import io.github.lishangbu.avalon.dataset.service.RegionService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service

@Service
class RegionServiceImpl(
    private val regionRepository: RegionRepository,
) : RegionService {
    override fun save(command: SaveRegionInput): RegionView = regionRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdateRegionInput): RegionView = regionRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    override fun removeById(id: Long) {
        regionRepository.deleteById(id)
    }

    override fun listByCondition(specification: RegionSpecification): List<RegionView> = regionRepository.listViews(specification)

    private fun reloadView(region: Region): RegionView = requireNotNull(regionRepository.loadViewById(region.id)) { "未找到 ID=${region.id} 对应的地区" }
}
