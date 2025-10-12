package io.github.lishangbu.avalon.admin.controller.dataset;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFirmnessService;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 树果坚硬度控制器
 *
 * @author lishangbu
 * @since 2025/10/5
 */
@RestController
@RequestMapping("/berry-firmness")
@RequiredArgsConstructor
public class BerryFirmnessController {
  private final BerryFirmnessService berryFirmnessService;

  @PostMapping("/import")
  public List<BerryFirmness> importBerryFirmnesses() {
    return berryFirmnessService.importBerryFirmnesses();
  }

  /**
   * 分页条件查询树果坚硬度
   *
   * @param pageable 分页参数（如 page, size, sort）
   * @param berryFirmness 查询条件，支持 name/internalName 模糊查询，其余字段精确匹配
   * @return 树果坚硬度分页结果
   */
  @GetMapping("/page")
  public Page<BerryFirmness> getBerryFirmnessPage(Pageable pageable, BerryFirmness berryFirmness) {
    return berryFirmnessService.getPageByCondition(berryFirmness, pageable);
  }

  /**
   * 新增树果坚硬度
   *
   * @param berryFirmness 树果坚硬度实体
   * @return 保存后的树果坚硬度
   */
  @PostMapping
  public BerryFirmness save(@RequestBody BerryFirmness berryFirmness) {
    return berryFirmnessService.save(berryFirmness);
  }

  /**
   * 更新树果坚硬度
   *
   * @param berryFirmness 树果坚硬度实体
   * @return 更新后的树果坚硬度
   */
  @PutMapping
  public BerryFirmness update(@RequestBody BerryFirmness berryFirmness) {
    return berryFirmnessService.update(berryFirmness);
  }

  /**
   * 根据ID删除树果坚硬度
   *
   * @param id 树果坚硬度ID
   */
  @DeleteMapping("/{id:\\d+}")
  public void deleteById(@PathVariable Long id) {
    berryFirmnessService.removeById(id);
  }

  /**
   * 条件查询树果坚硬度列表
   *
   * <p>支持按 name/internalName 模糊查询，其余字段精确匹配
   *
   * @param berryFirmness 查询条件，支持部分字段模糊查询
   * @return 树果坚硬度列表
   */
  @GetMapping("/list")
  public List<BerryFirmness> listBerryFirmnesses(BerryFirmness berryFirmness) {
    return berryFirmnessService.listByCondition(berryFirmness);
  }
}
