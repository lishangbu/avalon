package io.github.lishangbu.avalon.admin.controller;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.PageParam;
import io.github.lishangbu.avalon.admin.service.dataset.TypeService;
import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
   * 分页查询属性类型
   *
   * @param pageParam 分页参数
   * @param type 查询条件
   * @return 分页结果
   */
  @GetMapping("/page")
  public PageInfo<Type> getPage(PageParam pageParam, Type type) {
    return typeService.getPage(pageParam.getPageNum(), pageParam.getPageSize(), type);
  }
}
