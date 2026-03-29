package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 招式应用服务 */
interface MoveService {
    /** 按筛选条件分页查询招式 */
    fun getPageByCondition(
        specification: MoveSpecification,
        pageable: Pageable,
    ): Page<MoveView>

    /** 创建招式 */
    fun save(command: SaveMoveInput): MoveView

    /** 更新招式 */
    fun update(command: UpdateMoveInput): MoveView

    /** 删除指定 ID 的招式 */
    fun removeById(id: Long)
}
