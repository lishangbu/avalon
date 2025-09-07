package io.github.lishangbu.avalon.admin.controller;

import io.github.lishangbu.avalon.admin.service.dataset.TypeService;
import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
}
