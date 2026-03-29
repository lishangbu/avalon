package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.repository.ItemFlingEffectRepository
import io.github.lishangbu.avalon.dataset.service.ItemFlingEffectService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 道具投掷效果服务实现 */
@Service
class ItemFlingEffectServiceImpl(
    private val itemFlingEffectRepository: ItemFlingEffectRepository,
) : ItemFlingEffectService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveItemFlingEffectInput): ItemFlingEffectView = ItemFlingEffectView(itemFlingEffectRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateItemFlingEffectInput): ItemFlingEffectView = ItemFlingEffectView(itemFlingEffectRepository.save(command.toEntity(), SaveMode.UPSERT))

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        itemFlingEffectRepository.deleteById(id)
    }

    override fun listByCondition(specification: ItemFlingEffectSpecification): List<ItemFlingEffectView> = itemFlingEffectRepository.listViews(specification)
}
