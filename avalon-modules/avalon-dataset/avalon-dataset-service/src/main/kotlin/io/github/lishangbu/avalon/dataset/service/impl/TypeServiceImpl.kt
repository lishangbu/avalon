package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.SaveTypeInput
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.TypeView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateTypeInput
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import io.github.lishangbu.avalon.dataset.service.TypeService
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
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
    override fun save(command: SaveTypeInput): TypeView = TypeView(typeRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    /** 更新属性*/
    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateTypeInput): TypeView = TypeView(typeRepository.save(command.toEntity(), SaveMode.UPDATE_ONLY))

    /** 按 ID 删除属性*/
    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        typeRepository.deleteById(id)
    }

    /** 按条件查询属性列表*/
    override fun listByCondition(specification: TypeSpecification): List<TypeView> = typeRepository.listViews(specification)
}
