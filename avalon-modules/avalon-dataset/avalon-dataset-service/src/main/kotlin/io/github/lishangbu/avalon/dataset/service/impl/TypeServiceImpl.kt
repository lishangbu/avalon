package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import io.github.lishangbu.avalon.dataset.service.TypeService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 属性服务实现*/
@Service
class TypeServiceImpl(
    /** 属性仓储*/
    private val typeRepository: TypeRepository,
) : TypeService {
    /** 按条件分页查询属性*/
    override fun getPageByCondition(
        type: Type,
        pageable: Pageable,
    ): Page<Type> =
        typeRepository.findAll(
            Example.of(
                type,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

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
    override fun listByCondition(type: Type): List<Type> {
        val matcher =
            ExampleMatcher
                .matching()
                .withIgnoreNullValues()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains())
        return typeRepository.findAll(Example.of(type, matcher))
    }
}
