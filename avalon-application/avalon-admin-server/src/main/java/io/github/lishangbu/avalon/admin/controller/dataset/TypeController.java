package io.github.lishangbu.avalon.admin.controller.dataset;

import io.github.lishangbu.avalon.admin.service.dataset.TypeService;
import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 属性控制器
 *
 * @author lishangbu
 * @since 2025/8/24
 */
@RestController
@RequestMapping("/type")
@RequiredArgsConstructor
public class TypeController {
  private final TypeService typeService;

  /**
   * 导入数据源
   *
   * @return 属性列表
   */
  @PostMapping("/import")
  public List<Type> importTypes() {
    return typeService.importTypes();
  }

  /**
   * 分页条件查询属性类型
   *
   * @param pageable 分页参数（如 page, size, sort）
   * @param type 查询条件，支持 name/internalName 模糊查询，其余字段精确匹配
   * @return 属性类型分页结果
   */
  @GetMapping("/page")
  public Page<Type> getTypePage(Pageable pageable, Type type) {
    return typeService.getPageByCondition(type, pageable);
  }

  /**
   * 新增属性类型
   *
   * @param type 属性类型实体
   * @return 保存后的属性类型
   */
  @PostMapping
  public Type save(@RequestBody Type type) {
    return typeService.save(type);
  }

  /**
   * 更新属性类型
   *
   * @param type 属性类型实体
   * @return 更新后的属性类型
   */
  @PutMapping
  public Type update(@RequestBody Type type) {
    return typeService.update(type);
  }

  /**
   * 根据ID删除属性类型
   *
   * @param id 属性类型ID
   */
  @DeleteMapping("/{id:\\d+}")
  public void deleteById(@PathVariable Integer id) {
    typeService.removeById(id);
  }

  /**
   * 条件查询属性类型列表
   *
   * <p>支持按 name/internalName 模糊查询，其余字段精确匹配
   *
   * @param type 查询条件，支持部分字段模糊查询
   * @return 属性类型列表
   */
  @GetMapping("/list")
  public List<Type> listTypes(Type type) {
    return typeService.listByCondition(type);
  }
}
