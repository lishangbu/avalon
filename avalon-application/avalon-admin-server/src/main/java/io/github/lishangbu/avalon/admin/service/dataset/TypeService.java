package io.github.lishangbu.avalon.admin.service.dataset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;

/// 属性服务接口
///
/// 提供属性类型的导入、分页查询、CRUD 等操作
///
/// @author lishangbu
/// @since 2025/8/24
public interface TypeService {

  /// 根据条件分页查询属性类型
  ///
  /// @param page 分页参数
  /// @param type 查询条件
  /// @return 分页结果
  IPage<Type> getTypePage(Page<Type> page, Type type);

  /// 新增属性类型
  ///
  /// @param type 要保存的 Type 实体
  /// @return 保存后的 Type 实体
  Type save(Type type);

  /// 根据主键删除属性类型
  /// @param id 要删除的 Type 主键
  void removeById(Integer id);

  /// 更新属性类型
  /// @param type 要更新的 Type 实体
  /// @return 更新后的 Type 实体
  Type update(Type type);

  /// 根据条件查询属性类型列表，支持部分字段模糊查询
  /// @param type 查询条件，支持部分字段模糊查询
  /// @return 属性类型列表
  List<Type> listByCondition(Type type);
}
