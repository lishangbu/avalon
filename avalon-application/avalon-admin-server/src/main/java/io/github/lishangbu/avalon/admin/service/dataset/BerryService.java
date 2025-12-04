package io.github.lishangbu.avalon.admin.service.dataset;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 树果服务
 *
 * @author lishangbu
 * @since 2025/10/4
 */
public interface BerryService {

  /**
   * 导入树果
   *
   * @return 树果列表
   */
  List<Berry> importBerries();

  /**
   * 根据条件分页查询树果
   *
   * @param berry 查询条件
   * @param pageable 分页信息
   * @return 分页结果
   */
  Page<Berry> getPageByCondition(Berry berry, Pageable pageable);

  /**
   * 新增树果
   *
   * @param berry 要保存的树果实体
   * @return 保存后的树果实体
   */
  Berry save(Berry berry);

  /**
   * 根据主键删除树果
   *
   * @param id 要删除的树果主键
   */
  void removeById(Integer id);

  /**
   * 更新树果
   *
   * @param berry 要更新的树果实体
   * @return 更新后的树果实体
   */
  Berry update(Berry berry);
}
