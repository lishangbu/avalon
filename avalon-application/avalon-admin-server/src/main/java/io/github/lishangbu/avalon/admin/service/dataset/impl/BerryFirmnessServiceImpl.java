package io.github.lishangbu.avalon.admin.service.dataset.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.service.dataset.BerryFirmnessService;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.mapper.BerryFirmnessMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/// 树果坚硬度服务实现类
///
/// 提供树果坚硬度的数据导入、列表及 CRUD 操作
///
/// @author lishangbu
/// @since 2025/10/5
@Service
@RequiredArgsConstructor
public class BerryFirmnessServiceImpl implements BerryFirmnessService {
  private final BerryFirmnessMapper berryFirmnessMapper;

  @Override
  public IPage<BerryFirmness> getBerryFirmnessesPage(
      Page<BerryFirmness> page, BerryFirmness berryFirmness) {
    return berryFirmnessMapper.selectList(page, berryFirmness);
  }

  /// 根据条件查询树果坚硬度列表
  ///
  /// 支持按 name/internalName 模糊查询，其余字段精确匹配
  ///
  /// @param berryFirmness 查询条件，支持部分字段模糊查询
  /// @return 树果坚硬度列表
  @Override
  public List<BerryFirmness> listByCondition(BerryFirmness berryFirmness) {
    return berryFirmnessMapper.selectList(berryFirmness);
  }

  @Override
  public BerryFirmness save(BerryFirmness berryFirmness) {
    berryFirmnessMapper.insert(berryFirmness);
    return berryFirmness;
  }

  @Override
  public BerryFirmness update(BerryFirmness berryFirmness) {
    berryFirmnessMapper.updateById(berryFirmness);
    return berryFirmness;
  }

  @Override
  public void removeById(Integer id) {
    berryFirmnessMapper.deleteById(id);
  }
}
