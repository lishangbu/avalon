package io.github.lishangbu.avalon.admin.controller.dataset;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFlavorService;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/// 树果风味控制器
///
/// @author lishangbu
/// @since 2025/10/5
@RestController
@RequestMapping("/berry-flavor")
@RequiredArgsConstructor
public class BerryFlavorController {
    private final BerryFlavorService berryFlavorService;

    /// 分页条件查询树果风味
    ///
    /// @param pageable    分页参数（如 page, size, sort）
    /// @param berryFlavor 查询条件，支持 name/internalName 模糊查询，其余字段精确匹配
    /// @return 树果风味分页结果
    @GetMapping("/page")
    public Page<BerryFlavor> getBerryFlavorPage(Pageable pageable, BerryFlavor berryFlavor) {
        return berryFlavorService.getPageByCondition(berryFlavor, pageable);
    }

    /// 新增树果风味
    ///
    /// @param berryFlavor 树果风味实体
    /// @return 保存后的树果风味
    @PostMapping
    public BerryFlavor save(@RequestBody BerryFlavor berryFlavor) {
        return berryFlavorService.save(berryFlavor);
    }

    /// 更新树果风味
    ///
    /// @param berryFlavor 树果风味实体
    /// @return 更新后的树果风味
    @PutMapping
    public BerryFlavor update(@RequestBody BerryFlavor berryFlavor) {
        return berryFlavorService.update(berryFlavor);
    }

    /// 根据ID删除树果风味
    ///
    /// @param id 树果风味ID
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        berryFlavorService.removeById(id);
    }
}
