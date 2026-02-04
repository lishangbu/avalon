package io.github.lishangbu.avalon.admin.service.dataset.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.service.dataset.BerryFlavorService;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import io.github.lishangbu.avalon.dataset.mapper.BerryFlavorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/// 树果风味服务实现
///
/// 风味决定了宝可梦根据 [性格](https://pokeapi.co/docs/v2#natures) 食用树果时是受益还是受损。详情可参考
// [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Flavor)

///
/// @author lishangbu
/// @since 2025/10/5
@Service
@RequiredArgsConstructor
public class BerryFlavorServiceImpl implements BerryFlavorService {
  private final BerryFlavorMapper berryFlavorMapper;

  @Override
  public IPage<BerryFlavor> getBerryFlavorPage(Page<BerryFlavor> page, BerryFlavor berryFlavor) {
    return berryFlavorMapper.selectList(page, berryFlavor);
  }

  @Override
  public BerryFlavor save(BerryFlavor berryFlavor) {
    berryFlavorMapper.insert(berryFlavor);
    return berryFlavor;
  }

  @Override
  public BerryFlavor update(BerryFlavor berryFlavor) {
    berryFlavorMapper.updateById(berryFlavor);
    return berryFlavor;
  }

  @Override
  public void removeById(Integer id) {
    berryFlavorMapper.deleteById(id);
  }
}
