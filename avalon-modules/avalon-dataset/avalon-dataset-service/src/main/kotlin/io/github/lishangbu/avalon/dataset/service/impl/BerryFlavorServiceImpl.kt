package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 树果风味服务实现 */
@Service
class BerryFlavorServiceImpl(
    /** 树果风味仓储 */
    private val berryFlavorRepository: BerryFlavorRepository,
) : BerryFlavorService {
    /** 按条件分页查询树果风味*/
    override fun getPageByCondition(
        berryFlavor: BerryFlavor,
        pageable: Pageable,
    ): Page<BerryFlavor> =
        berryFlavorRepository.findAll(
            Example.of(
                berryFlavor,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("internalName", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    /** 保存树果风味 */
    override fun save(berryFlavor: BerryFlavor): BerryFlavor = berryFlavorRepository.save(berryFlavor)

    /** 更新树果风味 */
    override fun update(berryFlavor: BerryFlavor): BerryFlavor = berryFlavorRepository.save(berryFlavor)

    /** 按 ID 删除树果风味 */
    override fun removeById(id: Long) {
        berryFlavorRepository.deleteById(id)
    }
}
