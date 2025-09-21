package io.github.lishangbu.avalon.admin.controller;

import io.github.lishangbu.avalon.admin.service.dataset.TypeService;
import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
   * @param type 查询条件，支持 name/internalName 模糊查询
   * @return 属性类型分页结果
   */
  @GetMapping("/page")
  public Page<Type> getTypePage(Pageable pageable, Type type) {
    return typeService.getPageByCondition(type, pageable);
  }
}
