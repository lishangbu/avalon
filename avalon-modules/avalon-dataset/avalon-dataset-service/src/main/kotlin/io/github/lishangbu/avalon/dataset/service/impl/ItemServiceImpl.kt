package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Item
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemInput
import io.github.lishangbu.avalon.dataset.repository.ItemRepository
import io.github.lishangbu.avalon.dataset.service.ItemService
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 道具应用服务实现 */
@Service
class ItemServiceImpl(
    private val itemRepository: ItemRepository,
) : ItemService {
    override fun getPageByCondition(
        specification: ItemSpecification,
        pageable: Pageable,
    ): Page<ItemView> = itemRepository.pageViews(specification, pageable)

    override fun save(command: SaveItemInput): ItemView = itemRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    override fun update(command: UpdateItemInput): ItemView = itemRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY).let(::reloadView)

    override fun removeById(id: Long) {
        itemRepository.deleteById(id)
    }

    private fun reloadView(item: Item): ItemView = requireNotNull(itemRepository.loadViewById(item.id)) { "未找到 ID=${item.id} 对应的道具" }
}
