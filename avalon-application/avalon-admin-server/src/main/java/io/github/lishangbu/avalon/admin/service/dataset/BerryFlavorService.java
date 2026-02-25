package io.github.lishangbu.avalon.admin.service.dataset;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 树果风味服务
///
/// <p>风味决定了宝可梦根据<a href="https://pokeapi.co/docs/v2#natures">性格</a>食用树果时是受益还是受损。详情可参考<a
/// href="http://bulbapedia.bulbagarden.net/wiki/Flavor">Bulbapedia</a>
///
/// @author lishangbu
/// @since 2025/10/5
public interface BerryFlavorService {

    /// 分页条件查询树果风味
    ///
    /// @param berryFlavor 查询条件，支持 name/internalName 模糊查询，其余字段精确匹配
    /// @param pageable    分页参数
    /// @return 树果风味分页结果
    Page<BerryFlavor> getPageByCondition(BerryFlavor berryFlavor, Pageable pageable);

    /// 新增树果风味
    ///
    /// @param berryFlavor 树果风味实体
    /// @return 保存后的树果风味
    BerryFlavor save(BerryFlavor berryFlavor);

    /// 更新树果风味
    ///
    /// @param berryFlavor 树果风味实体
    /// @return 更新后的树果风味
    BerryFlavor update(BerryFlavor berryFlavor);

    /// 根据ID删除树果风味
    ///
    /// @param id 树果风味ID
    void removeById(Long id);
}
