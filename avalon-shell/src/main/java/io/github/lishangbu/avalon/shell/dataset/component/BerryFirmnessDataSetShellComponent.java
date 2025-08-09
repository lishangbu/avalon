package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.BERRY_FIRMNESS;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 树果数据集处理命令
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@ShellComponent
public class BerryFirmnessDataSetShellComponent extends AbstractDataSetShellComponent {

  private final PokeApiFactory pokeApiFactory;
  private final BerryFirmnessRepository berryFirmnessRepository;

  public BerryFirmnessDataSetShellComponent(
      PokeApiFactory pokeApiFactory, BerryFirmnessRepository berryFirmnessRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.berryFirmnessRepository = berryFirmnessRepository;
  }

  @ShellMethod(key = "dataset refresh berryFirmness", value = "刷新数据库中的树果硬度表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(BERRY_FIRMNESS);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToBerryFirmness,
        berryFirmnessRepository,
        BerryFirmness::getName);
  }

  private BerryFirmness convertToBerryFirmness(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness apiResult =
        pokeApiFactory.getSingleResource(
            BERRY_FIRMNESS, NamedApiResourceUtils.getId(namedApiResource));
    BerryFirmness berryFirmness = new BerryFirmness();
    berryFirmness.setId(apiResult.id());
    berryFirmness.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
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
