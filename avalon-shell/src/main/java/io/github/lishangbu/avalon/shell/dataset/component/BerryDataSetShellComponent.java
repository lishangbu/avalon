package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.BERRY;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
import io.github.lishangbu.avalon.dataset.repository.BerryRepository;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.transaction.annotation.Transactional;

/**
 * 树果数据集处理命令
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@ShellComponent
public class BerryDataSetShellComponent extends AbstractDataSetShellComponent {

  private final PokeApiFactory pokeApiFactory;
  private final BerryRepository berryRepository;
  private final BerryFirmnessRepository berryFirmnessRepository;

  private final TypeRepository typeRepository;

  public BerryDataSetShellComponent(
      PokeApiFactory pokeApiFactory,
      BerryRepository berryRepository,
      BerryFirmnessRepository berryFirmnessRepository,
      TypeRepository typeRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.berryRepository = berryRepository;
    this.berryFirmnessRepository = berryFirmnessRepository;
    this.typeRepository = typeRepository;
  }

  @ShellMethod(key = "dataset refresh berry", value = "刷新数据库中的树果表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(BERRY, offset, limit);
    return super.saveEntityData(
        namedApiResources.results(), this::convertToBerry, berryRepository, Berry::getName);
  }

  private Berry convertToBerry(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.berry.Berry apiResult =
        pokeApiFactory.getSingleResource(BERRY, namedApiResource.name());
    Berry berry = new Berry();
    berry.setId(apiResult.id());
    berry.setInternalName(apiResult.name());
    // TODO ITEM还没有完成，先取返回的英文名顶一下
    berry.setName(apiResult.name());
    berry.setSize(apiResult.size());
    berry.setGrowthTime(apiResult.growthTime());
    berry.setMaxHarvest(apiResult.maxHarvest());
    berry.setSmoothness(apiResult.smoothness());
    berry.setSoilDryness(apiResult.soilDryness());
    berry.setNaturalGiftPower(apiResult.naturalGiftPower());
    typeRepository
        .findByInternalName(apiResult.naturalGiftType().name())
        .ifPresent(berry::setNaturalGiftType);
    berryFirmnessRepository
        .findByInternalName(apiResult.firmness().name())
        .ifPresent(berry::setFirmness);
    return berry;
  }
}
