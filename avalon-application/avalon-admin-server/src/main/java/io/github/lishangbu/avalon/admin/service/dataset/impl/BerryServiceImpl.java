package io.github.lishangbu.avalon.admin.service.dataset.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.mapstruct.BerryMapstruct;
import io.github.lishangbu.avalon.admin.service.dataset.BerryService;
import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.mapper.BerryMapper;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Berry 服务实现
 *
 * @author lishangbu
 * @since 2025/10/4
 */
@Service
@RequiredArgsConstructor
public class BerryServiceImpl implements BerryService {

  private final BerryMapper berryMapper;

  private final PokeApiService pokeApiService;

  private final BerryMapstruct berryMapstruct;

  @Override
  public List<Berry> importBerries() {
    return pokeApiService.importData(
        PokeDataTypeEnum.BERRY,
        berryMapstruct::toDatasetBerry,
        berryMapper::insert,
        io.github.lishangbu.avalon.pokeapi.model.berry.Berry.class);
  }

  @Override
  public IPage<Berry> getBerryPage(Page<Berry> page, Berry berry) {
    return berryMapper.selectList(page, berry);
  }

  @Override
  public Berry save(Berry berry) {
    berryMapper.insert(berry);
    return berry;
  }

  @Override
  public void removeById(Integer id) {
    berryMapper.deleteById(id);
  }

  @Override
  public Berry update(Berry berry) {
    berryMapper.updateById(berry);
    return berry;
  }
}
