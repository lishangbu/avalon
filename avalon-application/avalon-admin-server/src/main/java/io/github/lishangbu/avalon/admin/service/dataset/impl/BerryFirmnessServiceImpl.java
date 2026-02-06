package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFirmnessService;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness_;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// 树果坚硬度服务实现类
///
/// @author lishangbu
/// @since 2025/10/5
@Service
@RequiredArgsConstructor
public class BerryFirmnessServiceImpl implements BerryFirmnessService {
  private final PokeApiService pokeApiService;
  private final BerryFirmnessRepository berryFirmnessRepository;

  /// 根据条件分页查询 BerryFirmness
  ///
  /// @param berryFirmness 查询条件
  /// @param pageable 分页信息
  /// @return 分页结果
  @Override
  public Page<BerryFirmness> getPageByCondition(BerryFirmness berryFirmness, Pageable pageable) {
    return berryFirmnessRepository.findAll(
        Example.of(
            berryFirmness,
            ExampleMatcher.matching()
                .withMatcher(BerryFirmness_.NAME, ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher(
                    BerryFirmness_.INTERNAL_NAME, ExampleMatcher.GenericPropertyMatchers.contains())
                .withIgnoreNullValues()),
        pageable);
  }

  /// 根据条件查询树果坚硬度列表
  ///
  /// <p>支持按 name/internalName 模糊查询，其余字段精确匹配
  ///
  /// @param berryFirmness 查询条件，支持部分字段模糊查询
  /// @return 树果坚硬度列表
  @Override
  public List<BerryFirmness> listByCondition(BerryFirmness berryFirmness) {
    ExampleMatcher matcher =
        ExampleMatcher.matching()
            .withIgnoreNullValues()
            .withMatcher(BerryFirmness_.NAME, ExampleMatcher.GenericPropertyMatchers.contains())
            .withMatcher(
                BerryFirmness_.INTERNAL_NAME, ExampleMatcher.GenericPropertyMatchers.contains());
    return berryFirmnessRepository.findAll(Example.of(berryFirmness, matcher));
  }

  /// 新增 BerryFirmness
  ///
  /// @param berryFirmness 要保存的 BerryFirmness 实体
  /// @return 保存后的 BerryFirmness 实体
  @Override
  public BerryFirmness save(BerryFirmness berryFirmness) {
    return berryFirmnessRepository.save(berryFirmness);
  }

  /// 更新 BerryFirmness
  ///
  /// @param berryFirmness 要更新的 BerryFirmness 实体
  /// @return 更新后的 BerryFirmness 实体
  @Override
  public BerryFirmness update(BerryFirmness berryFirmness) {
    return berryFirmnessRepository.save(berryFirmness);
  }

  /// 根据主键删除 BerryFirmness
  ///
  /// @param id 要删除的 BerryFirmness 主键
  @Override
  public void removeById(Long id) {
    berryFirmnessRepository.deleteById(id);
  }
}
