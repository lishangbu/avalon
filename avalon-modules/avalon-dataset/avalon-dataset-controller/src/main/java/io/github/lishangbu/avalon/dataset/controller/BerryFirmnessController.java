package io.github.lishangbu.avalon.dataset.controller;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 树果坚硬度控制器
///
/// @author lishangbu
/// @since 2026/3/11
@RestController
@RequestMapping("/berry-firmness")
@RequiredArgsConstructor
public class BerryFirmnessController {
    private final BerryFirmnessService berryFirmnessService;

    /// 分页条件查询树果坚硬度
    ///
    /// @param pageable      分页参数
    /// @param berryFirmness 查询条件，支持 name/internalName 模糊查询
    /// @return 树果坚硬度分页结果
    @GetMapping("/page")
    public Page<BerryFirmness> getBerryFirmnessPage(
            Pageable pageable, BerryFirmness berryFirmness) {
        return berryFirmnessService.getPageByCondition(berryFirmness, pageable);
    }

    /// 新增树果坚硬度
    ///
    /// @param berryFirmness 树果坚硬度实体
    /// @return 保存后的树果坚硬度
    @PostMapping
    public BerryFirmness save(@RequestBody BerryFirmness berryFirmness) {
        return berryFirmnessService.save(berryFirmness);
    }

    /// 更新树果坚硬度
    ///
    /// @param berryFirmness 树果坚硬度实体
    /// @return 更新后的树果坚硬度
    @PutMapping
    public BerryFirmness update(@RequestBody BerryFirmness berryFirmness) {
        return berryFirmnessService.update(berryFirmness);
    }

    /// 根据 ID 删除树果坚硬度
    ///
    /// @param id 树果坚硬度主键
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        berryFirmnessService.removeById(id);
    }

    /// 条件查询树果坚硬度列表
    ///
    /// @param berryFirmness 查询条件，支持 name/internalName 模糊查询
    /// @return 树果坚硬度列表
    @GetMapping("/list")
    public List<BerryFirmness> listBerryFirmnesses(BerryFirmness berryFirmness) {
        return berryFirmnessService.listByCondition(berryFirmness);
    }
}
