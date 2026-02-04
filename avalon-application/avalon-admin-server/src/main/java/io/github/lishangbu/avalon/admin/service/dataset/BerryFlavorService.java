package io.github.lishangbu.avalon.admin.service.dataset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import java.util.List;

/// 树果风味服务
///
/// 负责树果风味的导入、分页查询与 CRUD 操作
///
/// @author lishangbu
/// @since 2025/10/5
public interface BerryFlavorService {

  /// 分页条件查询树果风味
  ///
  /// @param berryFlavor 查询条件，支持 name/internalName 模糊查询，其余字段精确匹配
  /// @param page        分页参数
  /// @return 树果风味分页结果
  IPage<BerryFlavor> getBerryFlavorPage(Page<BerryFlavor> page, BerryFlavor berryFlavor);

  /// 新增树果风味
  ///
  /// @param berryFlavor 树果风味实体
  /// @return 保存后的树果风味
  BerryFlavor save(BerryFlavor berryFlavor);

  /// 更新树果风味
  /// @param berryFlavor 树果风味实体
  /// @return 更新后的树果风味
  BerryFlavor update(BerryFlavor berryFlavor);

  /// 根据 ID 删除树果风味
  /// @param id 树果风味 ID
  void removeById(Integer id);
}
