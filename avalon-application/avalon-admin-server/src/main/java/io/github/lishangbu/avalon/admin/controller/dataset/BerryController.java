package io.github.lishangbu.avalon.admin.controller.dataset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.service.dataset.BerryService;
import io.github.lishangbu.avalon.dataset.entity.Berry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/// Berry 控制器
///
/// 提供对树果资源的导入、分页查询及 CRUD 接口
///
/// @author lishangbu
/// @since 2025/10/4
@RestController
@RequestMapping("/berry")
@RequiredArgsConstructor
public class BerryController {
  private final BerryService berryService;

  /// 分页条件查询树果
  ///
  /// @param page  分页参数
  /// @param berry 查询条件
  /// @return Berry 分页结果
  @GetMapping("/page")
  public IPage<Berry> getBerryPage(Page<Berry> page, Berry berry) {
    return berryService.getBerryPage(page, berry);
  }

  /// 新增树果
  ///
  /// @param berry 待保存的树果实体
  /// @return 保存后的 Berry
  @PostMapping
  public Berry save(@RequestBody Berry berry) {
    return berryService.save(berry);
  }

  /// 更新树果
  /// @param berry 待更新的树果实体
  /// @return 更新后的树果
  @PutMapping
  public Berry update(@RequestBody Berry berry) {
    return berryService.update(berry);
  }

  /// 根据 ID 删除树果
  /// @param id Berry 主键
  @DeleteMapping("/{id:\\d+}")
  public void deleteById(@PathVariable Integer id) {
    berryService.removeById(id);
  }
}
