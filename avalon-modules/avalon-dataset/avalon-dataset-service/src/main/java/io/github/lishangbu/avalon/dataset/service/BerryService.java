package io.github.lishangbu.avalon.dataset.service;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 树果服务。
public interface BerryService {

    /// 根据条件分页查询树果。
    Page<Berry> getPageByCondition(Berry berry, Pageable pageable);

    /// 新增树果。
    Berry save(Berry berry);

    /// 更新树果。
    Berry update(Berry berry);

    /// 根据主键删除树果。
    void removeById(Long id);
}
