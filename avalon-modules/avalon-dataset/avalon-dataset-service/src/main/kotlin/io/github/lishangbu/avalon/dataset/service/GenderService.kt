package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification

/** 性别服务 */
interface GenderService {
    /** 创建性别 */
    fun save(gender: Gender): Gender

    /** 更新性别 */
    fun update(gender: Gender): Gender

    /** 删除指定 ID 的性别 */
    fun removeById(id: Long)

    /** 按筛选条件查询性别列表 */
    fun listByCondition(specification: GenderSpecification): List<Gender>
}
