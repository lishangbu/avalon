package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.ItemCategory
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemCategoryInput
import io.github.lishangbu.avalon.dataset.repository.ItemCategoryRepository
import io.github.lishangbu.avalon.dataset.service.ItemCategoryService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 道具类别服务实现 */
@Service
class ItemCategoryServiceImpl(
    private val itemCategoryRepository: ItemCategoryRepository,
) : ItemCategoryService {
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveItemCategoryInput): ItemCategoryView = itemCategoryRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateItemCategoryInput): ItemCategoryView = itemCategoryRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        itemCategoryRepository.deleteById(id)
    }

    override fun listByCondition(specification: ItemCategorySpecification): List<ItemCategoryView> = itemCategoryRepository.listViews(specification)

    private fun reloadView(itemCategory: ItemCategory): ItemCategoryView = requireNotNull(itemCategoryRepository.loadViewById(itemCategory.id)) { "未找到 ID=${itemCategory.id} 对应的道具类别" }
}
