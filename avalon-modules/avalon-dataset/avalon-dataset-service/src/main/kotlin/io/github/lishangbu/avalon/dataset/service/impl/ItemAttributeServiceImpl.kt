package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemAttributeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemAttributeInput
import io.github.lishangbu.avalon.dataset.repository.ItemAttributeRepository
import io.github.lishangbu.avalon.dataset.service.ItemAttributeService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 道具属性服务实现 */
@Service
class ItemAttributeServiceImpl(
    private val itemAttributeRepository: ItemAttributeRepository,
) : ItemAttributeService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveItemAttributeInput): ItemAttributeView = ItemAttributeView(itemAttributeRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateItemAttributeInput): ItemAttributeView = ItemAttributeView(itemAttributeRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        itemAttributeRepository.deleteById(id)
    }

    override fun listByCondition(specification: ItemAttributeSpecification): List<ItemAttributeView> = itemAttributeRepository.listViews(specification)
}
