package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/// 树果风味(BerryFlavor)数据访问层
///
/// 提供树果风味数据的 CRUD 操作，继承 MyBatis-Plus BaseMapper，并包含自定义的查询方法
///
/// @author lishangbu
/// @since 2025/09/14
@Mapper
public interface BerryFlavorMapper extends BaseMapper<BerryFlavor> {

  /// 分页查询树果风味列表（支持动态条件）
  ///
  /// 根据提供的查询条件和分页参数进行分页查询，支持按 ID、内部名称、显示名称等条件筛选，按 ID 升序排列返回分页结果
  ///
  /// @param page        分页参数，包含页码、每页大小等信息
  /// @param berryFlavor 查询条件对象，包含筛选条件
  /// @return 分页结果，包含符合条件的树果风味列表和分页信息
  IPage<BerryFlavor> selectList(
      @Param("page") Page<BerryFlavor> page, @Param("berryFlavor") BerryFlavor berryFlavor);

  /// 查询树果风味列表（支持动态条件）
  ///
  /// 根据提供的查询条件动态筛选树果风味数据，支持按 ID、内部名称、显示名称等条件进行查询，按 ID 升序排列返回
  ///
  /// @param berryFlavor 查询条件对象，包含筛选条件
  /// @return 树果风味列表，符合查询条件的树果风味信息
  List<BerryFlavor> selectList(@Param("berryFlavor") BerryFlavor berryFlavor);
}
