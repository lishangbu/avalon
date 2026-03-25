package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.repository.GenderRepository
import io.github.lishangbu.avalon.dataset.service.GenderService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 性别服务实现 */
@Service
class GenderServiceImpl(
    /** 性别仓储 */
    private val genderRepository: GenderRepository,
) : GenderService {
    /** 保存性别 */
    @Transactional(rollbackFor = [Exception::class])
    override fun save(gender: Gender): Gender = genderRepository.save(gender)

    /** 更新性别 */
    @Transactional(rollbackFor = [Exception::class])
    override fun update(gender: Gender): Gender = genderRepository.save(gender)

    /** 按 ID 删除性别 */
    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        genderRepository.deleteById(id)
    }

    /** 按条件查询性别列表 */
    override fun listByCondition(specification: GenderSpecification): List<Gender> = genderRepository.findAll(specification)
}
