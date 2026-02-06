package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.BerryFlavorService;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor_;
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// 树果风味服务实现
///
/// <p>风味决定了宝可梦根据<a href="https://pokeapi.co/docs/v2#natures">性格</a>食用树果时是受益还是受损。详情可参考<a
/// href="http://bulbapedia.bulbagarden.net/wiki/Flavor">Bulbapedia</a>
///
/// @author lishangbu
/// @since 2025/10/5
@Service
@RequiredArgsConstructor
public class BerryFlavorServiceImpl implements BerryFlavorService {

  private final BerryFlavorRepository berryFlavorRepository;

  /// 根据条件分页查询 BerryFlavor
  ///
  /// @param berryFlavor 查询条件
  /// @param pageable 分页信息
  /// @return 分页结果
  @Override
  public Page<BerryFlavor> getPageByCondition(BerryFlavor berryFlavor, Pageable pageable) {
    return berryFlavorRepository.findAll(
        Example.of(
            berryFlavor,
            ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withMatcher(BerryFlavor_.NAME, ExampleMatcher.GenericPropertyMatchers.contains())
                .withMatcher(
                    BerryFlavor_.INTERNAL_NAME, ExampleMatcher.GenericPropertyMatchers.contains())),
        pageable);
  }

  /// 新增 BerryFlavor
  ///
  /// @param berryFlavor 要保存的 BerryFlavor 实体
  /// @return 保存后的 BerryFlavor 实体
  @Override
  public BerryFlavor save(BerryFlavor berryFlavor) {
    return berryFlavorRepository.save(berryFlavor);
  }

  /// 更新 BerryFlavor
  ///
  /// @param berryFlavor 要更新的 BerryFlavor 实体
  /// @return 更新后的 BerryFlavor 实体
  @Override
  public BerryFlavor update(BerryFlavor berryFlavor) {
    return berryFlavorRepository.save(berryFlavor);
  }

  /// 根据主键删除 BerryFlavor
  ///
  /// @param id 要删除的 BerryFlavor 主键
  @Override
  public void removeById(Long id) {
    berryFlavorRepository.deleteById(id);
  }
}
