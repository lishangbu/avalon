package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 道具应用服务 */
interface ItemService {
    /** 按筛选条件分页查询道具 */
    fun getPageByCondition(
        specification: ItemSpecification,
        pageable: Pageable,
    ): Page<ItemView>

    /** 创建道具 */
    fun save(command: SaveItemInput): ItemView

    /** 更新道具 */
    fun update(command: UpdateItemInput): ItemView

    /** 删除指定 ID 的道具 */
    fun removeById(id: Long)
}
