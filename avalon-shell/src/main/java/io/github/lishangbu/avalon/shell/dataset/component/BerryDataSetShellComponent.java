package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.repository.BerryRepository;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * 树果数据集处理命令
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@ShellComponent
public class BerryDataSetShellComponent extends AbstractDataSetShellComponent {

  private final PokeApiService pokeApiService;
  public final BerryRepository berryRepository;

  private final TypeRepository typeRepository;

  public BerryDataSetShellComponent(
      PokeApiService pokeApiService,
      BerryRepository berryRepository,
      TypeRepository typeRepository) {
    this.pokeApiService = pokeApiService;
    this.berryRepository = berryRepository;
    this.typeRepository = typeRepository;
  }

  @ShellMethod(key = "dataset refresh berry", value = "刷新数据库中的树果表数据")
  @Override
  public String refreshData(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources = pokeApiService.listBerries(offset, limit);
    return super.saveSingleEntityData(
        namedApiResources.results(), this::convertToBerry, berryRepository, Berry::getName);
  }

  private Berry convertToBerry(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.berry.Berry apiResult =
        pokeApiService.getBerry(namedApiResource.name());
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
    return berry;
  }

  @ShellMethod(key = "dataset refresh berry", value = "刷新数据库中的树果表数据")
  public String refreshBerries(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources = pokeApiService.listBerries(offset, limit);
    List<Berry> berries = new ArrayList<>();
    for (NamedApiResource namedApiResource : namedApiResources.results()) {
      io.github.lishangbu.avalon.pokeapi.model.berry.Berry result =
          pokeApiService.getBerry(namedApiResource.name());
      Berry berry = new Berry();
      berry.setId(result.id());
      berry.setInternalName(result.name());
      // TODO ITEM还没有完成，先取返回的英文名顶一下
      berry.setName(result.name());
      berry.setSize(result.size());
      berry.setGrowthTime(result.growthTime());
      berry.setMaxHarvest(result.maxHarvest());
      berry.setSmoothness(result.smoothness());
      berry.setSoilDryness(result.soilDryness());
      berry.setNaturalGiftPower(result.naturalGiftPower());
      typeRepository
          .findByInternalName(result.naturalGiftType().name())
          .ifPresent(
              type -> {
                berry.setNaturalGiftType(type);
              });
      berries.add(berry);
    }
    berryRepository.saveAllAndFlush(berries);
    return String.format(
        "数据处理完成，本次处理从API接口获取了数据:%s。\r\n当前共有数据:%d条",
        berries.stream().map(Berry::getName).collect(Collectors.joining(",")),
        berryRepository.count());
  }
}
