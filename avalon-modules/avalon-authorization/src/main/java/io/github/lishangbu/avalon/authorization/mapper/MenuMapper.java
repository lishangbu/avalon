package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.authorization.entity.Menu;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 菜单表的 MyBatis-Plus Mapper
 *
 * <p>提供对 Menu 实体的基本 CRUD 操作，并提供基于角色代码查询菜单的自定义方法（由 XML 实现）
 */
public interface MenuMapper extends BaseMapper<Menu> {
  /**
   * 根据角色代码列表查询对应的菜单集合，由 mapper XML 实现
   *
   * @param roleCodes 角色代码列表
   * @return 匹配的菜单列表
   */
  List<Menu> selectByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
