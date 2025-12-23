package io.github.lishangbu.avalon.admin.service.dataset.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.mapstruct.BerryFlavorMapstruct;
import io.github.lishangbu.avalon.admin.service.dataset.BerryFlavorService;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import io.github.lishangbu.avalon.dataset.mapper.BerryFlavorMapper;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 树果风味服务实现
 *
 * <p>风味决定了宝可梦根据<a href="https://pokeapi.co/docs/v2#natures">性格</a>食用树果时是受益还是受损。详情可参考<a
 * href="http://bulbapedia.bulbagarden.net/wiki/Flavor">Bulbapedia</a>
 *
 * @author lishangbu
 * @since 2025/10/5
 */
@Service
@RequiredArgsConstructor
public class BerryFlavorServiceImpl implements BerryFlavorService {
  private final PokeApiService pokeApiService;

  private final BerryFlavorMapper berryFlavorMapper;

  private final BerryFlavorMapstruct berryFlavorMapstruct;

  @Override
  public List<BerryFlavor> importBerryFlavors() {
    return pokeApiService.importData(
        PokeDataTypeEnum.BERRY_FLAVOR,
        berryFlavorMapstruct::toDatasetBerryFlavor,
        berryFlavorMapper::insert,
        io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor.class);
  }

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
