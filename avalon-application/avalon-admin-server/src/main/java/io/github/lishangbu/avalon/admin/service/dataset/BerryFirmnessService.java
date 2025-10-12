package io.github.lishangbu.avalon.admin.service.dataset;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 树果坚硬度服务
 *
 * @author lishangbu
 * @since 2025/10/5
 */
public interface BerryFirmnessService {
  /**
   * 导入树果坚硬度列表
   *
   * @return 树果坚硬度列表
   */
  List<BerryFirmness> importBerryFirmnesses();

  /**
   * 分页条件查询树果坚硬度
   *
   * @param berryFirmness 查询条件，支持 name/internalName 模糊查询，其余字段精确匹配
   * @param pageable 分页参数
   * @return 树果坚硬度分页结果
   */
  Page<BerryFirmness> getPageByCondition(BerryFirmness berryFirmness, Pageable pageable);

  /**
   * 新增树果坚硬度
   *
   * @param berryFirmness 树果坚硬度实体
   * @return 保存后的树果坚硬度
   */
  BerryFirmness save(BerryFirmness berryFirmness);

  /**
   * 更新树果坚硬度
   *
   * @param berryFirmness 树果坚硬度实体
   * @return 更新后的树果坚硬度
   */
  BerryFirmness update(BerryFirmness berryFirmness);

  /**
   * 根据ID删除树果坚硬度
   *
   * @param id 树果坚硬度ID
   */
  void removeById(Long id);

  /**
   * 根据条件查询树果坚硬度列表
   * <p>
   * 支持按 name/internalName 模糊查询，其余字段精确匹配
   *
   * @param berryFirmness 查询条件，支持部分字段模糊查询
   * @return 树果坚硬度列表
   */
  List<BerryFirmness> listByCondition(BerryFirmness berryFirmness);
}
