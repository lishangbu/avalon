package io.github.lishangbu.avalon.dataset.controller;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.service.BerryService;
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

/// 树果控制器
///
/// @author lishangbu
/// @since 2026/3/11
@RestController
@RequestMapping("/berry")
@RequiredArgsConstructor
public class BerryController {
    private final BerryService berryService;

    /// 分页条件查询树果
    ///
    /// @param pageable 分页参数
    /// @param berry    查询条件
    /// @return 树果分页结果
    @GetMapping("/page")
    public Page<Berry> getBerryPage(Pageable pageable, Berry berry) {
        return berryService.getPageByCondition(berry, pageable);
    }

    /// 新增树果
    ///
    /// @param berry 待保存的树果实体
    /// @return 保存后的树果
    @PostMapping
    public Berry save(@RequestBody Berry berry) {
        return berryService.save(berry);
    }

    /// 更新树果
    ///
    /// @param berry 待更新的树果实体
    /// @return 更新后的树果
    @PutMapping
    public Berry update(@RequestBody Berry berry) {
        return berryService.update(berry);
    }

    /// 根据 ID 删除树果
    ///
    /// @param id 树果主键
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        berryService.removeById(id);
    }
}
