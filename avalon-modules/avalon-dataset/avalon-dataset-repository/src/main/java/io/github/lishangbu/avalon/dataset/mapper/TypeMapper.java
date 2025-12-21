package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 属性(Type)数据访问层
 *
 * <p>提供属性数据的 CRUD 操作 继承 MyBatis-Plus BaseMapper，自动获得基础的增删改查方法 包含自定义的查询方法用于业务需求
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Mapper
public interface TypeMapper extends BaseMapper<Type> {

  /**
   * 查询属性列表（支持动态条件）
   *
   * <p>根据提供的查询条件动态筛选属性数据 支持按 ID、内部名称、显示名称等条件进行查询 按 ID 升序排列返回
   *
   * @return 属性列表，符合查询条件的属性类型信息
   */
  List<Type> selectList(Type type);

  /**
   * 分页查询属性列表（支持动态条件）
   *
   * <p>根据提供的查询条件和分页参数进行分页查询 支持按 ID、内部名称、显示名称等条件进行筛选 按 ID 升序排列返回分页结果
   *
   * @param page 分页参数，包含页码、每页大小等信息
   * @param type 查询条件对象，包含筛选条件
   * @return 分页结果，包含符合条件的属性列表和分页信息
   */
  IPage<Type> selectList(Page<Type> page, Type type);
}
