package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Nature
import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.NatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveNatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateNatureInput
import io.github.lishangbu.avalon.dataset.repository.NatureRepository
import io.github.lishangbu.avalon.dataset.service.NatureService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 性格服务实现 */
@Service
class NatureServiceImpl(
    private val natureRepository: NatureRepository,
) : NatureService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveNatureInput): NatureView = natureRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateNatureInput): NatureView = natureRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        natureRepository.removeById(id)
    }

    override fun listByCondition(specification: NatureSpecification): List<NatureView> = natureRepository.findAll(specification).map(::NatureView)

    private fun reloadView(nature: Nature): NatureView = NatureView(requireNotNull(natureRepository.findByIdWithAssociations(nature.id)) { "未找到 ID=${nature.id} 对应的性格" })
}
