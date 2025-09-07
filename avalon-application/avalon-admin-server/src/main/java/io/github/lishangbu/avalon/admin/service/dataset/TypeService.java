package io.github.lishangbu.avalon.admin.service.dataset;

import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;

/**
 * 属性服务
 *
 * @author lishangbu
 * @since 2025/8/24
 */
public interface TypeService {

  /**
   * 导入属性类型
   *
   * @return 属性类型列表
   */
  List<Type> importTypes();

  /**
   * 新增属性类型
   *
   * @param type 要保存的 Type 实体
   * @return 保存后的 Type 实体
   */
  Type save(Type type);

  /**
   * 根据主键删除属性类型
   *
   * @param id 要删除的 Type 主键
   */
  void deleteById(Long id);

  /**
   * 更新属性类型
   *
   * @param type 要更新的 Type 实体
   * @return 更新后的 Type 实体
   */
  Type update(Type type);

  /**
   * 根据主键查询属性类型
   *
   * @param id Type 主键
   * @return 查询到的 Type 实体，未找到时返回 null
   */
  Type findById(Long id);
}
