package io.github.lishangbu.avalon.dataset.service;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 树果风味服务。
public interface BerryFlavorService {

    /// 根据条件分页查询树果风味。
    Page<BerryFlavor> getPageByCondition(BerryFlavor berryFlavor, Pageable pageable);

    /// 新增树果风味。
    BerryFlavor save(BerryFlavor berryFlavor);

    /// 更新树果风味。
    BerryFlavor update(BerryFlavor berryFlavor);

    /// 根据主键删除树果风味。
    void removeById(Long id);
}
