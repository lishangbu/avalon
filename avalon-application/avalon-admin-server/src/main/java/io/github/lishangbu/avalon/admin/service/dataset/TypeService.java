package io.github.lishangbu.avalon.admin.service.dataset;

import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
   * 根据条件分页查询属性类型
   *
   * @param type 查询条件
   * @param pageable 分页信息
   * @return 分页结果
   */
  Page<Type> getPageByCondition(Type type, Pageable pageable);

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
  void removeById(Long id);

  /**
   * 更新属性类型
   *
   * @param type 要更新的 Type 实体
   * @return 更新后的 Type 实体
   */
  Type update(Type type);

}
