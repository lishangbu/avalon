package io.github.lishangbu.avalon.admin.controller.dataset;

import io.github.lishangbu.avalon.admin.service.dataset.BerryService;
import io.github.lishangbu.avalon.dataset.entity.Berry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * Berry 控制器
 *
 * @author lishangbu
 * @since 2025/10/4
 */
@RestController
@RequestMapping("/berry")
@RequiredArgsConstructor
public class BerryController {
  private final BerryService berryService;

  /**
   * 导入数据源
   *
   * @return 属性列表
   */
  @PostMapping("/import")
  public List<Berry> importBerries() {
    return berryService.importBerries();
  }

  /**
   * 分页条件查询树果
   *
   * @param pageable 分页参数
   * @param berry 查询条件
   * @return Berry 分页结果
   */
  @GetMapping("/page")
  public Page<Berry> getBerryPage(Pageable pageable, Berry berry) {
    return berryService.getPageByCondition(berry, pageable);
  }

  /**
   * 新增树果
   *
   * @param berry 待保存的树果实体
   * @return 保存后的 Berry
   */
  @PostMapping
  public Berry save(@RequestBody Berry berry) {
    return berryService.save(berry);
  }

  /**
   * 更新树果
   *
   * @param berry 待更新的树果实体
   * @return 更新后的树果
   */
  @PutMapping
  public Berry update(@RequestBody Berry berry) {
    return berryService.update(berry);
  }

  /**
   * 根据 ID 删除树果
   *
   * @param id Berry 主键
   */
  @DeleteMapping("/{id:\\d+}")
  public void deleteById(@PathVariable Long id) {
    berryService.removeById(id);
  }
}
