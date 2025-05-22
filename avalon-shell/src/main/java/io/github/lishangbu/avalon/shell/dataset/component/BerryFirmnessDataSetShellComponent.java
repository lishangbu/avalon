package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
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
public class BerryFirmnessDataSetShellComponent extends AbstractDataSetShellComponent {

  private final PokeApiService pokeApiService;
  public final BerryFirmnessRepository berryFirmnessRepository;

  public BerryFirmnessDataSetShellComponent(
      PokeApiService pokeApiService, BerryFirmnessRepository berryFirmnessRepository) {
    this.pokeApiService = pokeApiService;
    this.berryFirmnessRepository = berryFirmnessRepository;
  }

  @ShellMethod(key = "dataset refresh berryFirmness", value = "刷新数据库中的树果硬度表数据")
  @Override
  public String refreshData(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources = pokeApiService.listBerryFirmnesses(offset, limit);
    return super.saveSingleEntityData(
        namedApiResources.results(),
        this::convertToBerryFirmness,
        berryFirmnessRepository,
        BerryFirmness::getName);
  }

  private BerryFirmness convertToBerryFirmness(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness apiResult =
        pokeApiService.getBerryFirmness(namedApiResource.name());
    BerryFirmness berryFirmness = new BerryFirmness();
    berryFirmness.setId(apiResult.id());
    berryFirmness.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names(), LocalizationUtils.SIMPLIFIED_CHINESE)
        .ifPresentOrElse(
            name -> {
              berryFirmness.setName(name.name());
            },
            () -> {
              berryFirmness.setName(apiResult.name());
            });
    return berryFirmness;
  }
}
