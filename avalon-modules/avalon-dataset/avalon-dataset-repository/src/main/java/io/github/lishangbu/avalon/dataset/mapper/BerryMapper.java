package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.Berry;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/// 树果(Berry)数据访问层
///
/// 提供树果数据的 CRUD 操作，继承 MyBatis-Plus BaseMapper，并包含自定义查询方法
///
/// @author lishangbu
/// @since 2025/09/14
@Mapper
public interface BerryMapper extends BaseMapper<Berry> {

  /// 查询树果列表（支持动态条件）
  ///
  /// @param berry 查询条件对象，包含筛选条件
  /// @return 树果列表，符合查询条件的树果信息
  List<Berry> selectList(@Param("berry") Berry berry);

  /// 分页查询树果列表（支持动态条件）
  ///
  /// @param page  分页参数，包含页码、每页大小等信息
  /// @param berry 查询条件对象，包含筛选条件
  /// @return 分页结果，包含符合条件的树果列表和分页信息
  IPage<Berry> selectList(@Param("page") Page<Berry> page, @Param("berry") Berry berry);
}
