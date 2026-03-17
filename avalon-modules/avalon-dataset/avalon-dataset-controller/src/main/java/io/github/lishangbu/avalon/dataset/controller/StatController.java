package io.github.lishangbu.avalon.dataset.controller;

import io.github.lishangbu.avalon.dataset.entity.Stat;
import io.github.lishangbu.avalon.dataset.service.StatService;
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

/// 能力(Stat)控制器
///
/// @author lishangbu
/// @since 2026/3/19
@RestController
@RequestMapping("/stat")
@RequiredArgsConstructor
public class StatController {
    private final StatService statService;

    /// 分页条件查询能力
    ///
    /// @param pageable 分页参数
    /// @param stat     查询条件，支持 name/internalName 模糊查询
    /// @return 能力分页结果
    @GetMapping("/page")
    public Page<Stat> getStatPage(Pageable pageable, Stat stat) {
        return statService.getPageByCondition(stat, pageable);
    }

    /// 新增能力
    ///
    /// @param stat 能力实体
    /// @return 保存后的能力
    @PostMapping
    public Stat save(@RequestBody Stat stat) {
        return statService.save(stat);
    }

    /// 更新能力
    ///
    /// @param stat 能力实体
    /// @return 更新后的能力
    @PutMapping
    public Stat update(@RequestBody Stat stat) {
        return statService.update(stat);
    }

    /// 根据 ID 删除能力
    ///
    /// @param id 能力主键
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        statService.removeById(id);
    }

    /// 条件查询能力列表
    ///
    /// @param stat 查询条件，支持 name/internalName 模糊查询
    /// @return 能力列表
    @GetMapping("/list")
    public List<Stat> listStats(Stat stat) {
        return statService.listByCondition(stat);
    }
}
