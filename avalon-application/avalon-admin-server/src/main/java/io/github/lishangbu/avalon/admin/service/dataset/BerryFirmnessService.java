package io.github.lishangbu.avalon.admin.service.dataset;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import java.util.List;

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
}
