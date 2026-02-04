package io.github.lishangbu.avalon.admin.service.dataset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.Berry;
import java.util.List;

/// 树果服务
///
/// 提供树果的导入、分页查询及 CRUD 操作
///
/// @author lishangbu
/// @since 2025/10/4
public interface BerryService {

  /// 根据条件分页查询树果
  ///
  /// @param page  分页信息
  /// @param berry 查询条件
  /// @return 分页结果
  IPage<Berry> getBerryPage(Page<Berry> page, Berry berry);

  /// 新增树果
  ///
  /// @param berry 要保存的树果实体
  /// @return 保存后的树果实体
  Berry save(Berry berry);

  /// 根据主键删除树果
  /// @param id 要删除的树果主键
  void removeById(Integer id);

  /// 更新树果
  /// @param berry 要更新的树果实体
  /// @return 更新后的树果实体
  Berry update(Berry berry);
}
