package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.repository.BerryRepository
import io.github.lishangbu.avalon.dataset.service.BerryService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * 树果应用服务实现
 *
 * 基于仓储封装树果的分页查询与写入逻辑
 */
@Service
class BerryServiceImpl(
    /** 树果仓储 */
    private val berryRepository: BerryRepository,
) : BerryService {
    /** 按筛选条件分页查询树果*/
    override fun getPageByCondition(
        berry: Berry,
        pageable: Pageable,
    ): Page<Berry> =
        berryRepository.findAll(
            Example.of(
                berry,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    /** 创建树果 */
    override fun save(berry: Berry): Berry = berryRepository.save(berry)

    /** 更新树果 */
    override fun update(berry: Berry): Berry = berryRepository.save(berry)

    /** 删除指定 ID 的树果*/
    override fun removeById(id: Long) {
        berryRepository.deleteById(id)
    }
}
