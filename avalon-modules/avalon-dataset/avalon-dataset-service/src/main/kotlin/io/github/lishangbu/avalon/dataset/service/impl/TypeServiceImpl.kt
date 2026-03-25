package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import io.github.lishangbu.avalon.dataset.service.TypeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 属性服务实现*/
@Service
class TypeServiceImpl(
    /** 属性仓储*/
    private val typeRepository: TypeRepository,
) : TypeService {
    /** 保存属性*/
    @Transactional(rollbackFor = [Exception::class])
    override fun save(type: Type): Type = typeRepository.save(type)

    /** 更新属性*/
    @Transactional(rollbackFor = [Exception::class])
    override fun update(type: Type): Type = typeRepository.save(type)

    /** 按 ID 删除属性*/
    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        typeRepository.deleteById(id)
    }

    /** 按条件查询属性列表*/
    override fun listByCondition(specification: TypeSpecification): List<Type> = typeRepository.findAll(specification)
}
