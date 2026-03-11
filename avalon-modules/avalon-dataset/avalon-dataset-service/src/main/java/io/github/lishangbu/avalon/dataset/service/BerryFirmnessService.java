package io.github.lishangbu.avalon.dataset.service;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 树果坚硬度服务。
public interface BerryFirmnessService {

    /// 根据条件分页查询树果坚硬度。
    Page<BerryFirmness> getPageByCondition(BerryFirmness berryFirmness, Pageable pageable);

    /// 新增树果坚硬度。
    BerryFirmness save(BerryFirmness berryFirmness);

    /// 更新树果坚硬度。
    BerryFirmness update(BerryFirmness berryFirmness);

    /// 根据主键删除树果坚硬度。
    void removeById(Long id);

    /// 根据条件查询树果坚硬度列表。
    List<BerryFirmness> listByCondition(BerryFirmness berryFirmness);
}
