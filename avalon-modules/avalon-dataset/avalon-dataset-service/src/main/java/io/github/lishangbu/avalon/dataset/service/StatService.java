package io.github.lishangbu.avalon.dataset.service;

import io.github.lishangbu.avalon.dataset.entity.Stat;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 能力(Stat)服务。
public interface StatService {

    /// 根据条件分页查询能力。
    Page<Stat> getPageByCondition(Stat stat, Pageable pageable);

    /// 新增能力。
    Stat save(Stat stat);

    /// 更新能力。
    Stat update(Stat stat);

    /// 根据主键删除能力。
    void removeById(Long id);

    /// 根据条件查询能力列表。
    List<Stat> listByCondition(Stat stat);
}
