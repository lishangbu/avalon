package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemPocketInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemPocketInput
import io.github.lishangbu.avalon.dataset.repository.ItemPocketRepository
import io.github.lishangbu.avalon.dataset.service.ItemPocketService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 道具口袋服务实现 */
@Service
class ItemPocketServiceImpl(
    private val itemPocketRepository: ItemPocketRepository,
) : ItemPocketService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveItemPocketInput): ItemPocketView = ItemPocketView(itemPocketRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateItemPocketInput): ItemPocketView = ItemPocketView(itemPocketRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        itemPocketRepository.deleteById(id)
    }

    override fun listByCondition(specification: ItemPocketSpecification): List<ItemPocketView> = itemPocketRepository.listViews(specification)
}
