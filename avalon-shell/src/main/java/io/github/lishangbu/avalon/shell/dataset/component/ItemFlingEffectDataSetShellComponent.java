package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.ITEM_FLING_EFFECT;

import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect;
import io.github.lishangbu.avalon.dataset.repository.ItemFlingEffectRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 道具投掷效果数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class ItemFlingEffectDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final ItemFlingEffectRepository itemFlingEffectRepository;

  public ItemFlingEffectDataSetShellComponent(
      PokeApiFactory pokeApiFactory, ItemFlingEffectRepository itemFlingEffectRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.itemFlingEffectRepository = itemFlingEffectRepository;
  }

  @ShellMethod(key = "dataset refresh itemFlingEffect", value = "刷新数据库中的道具投掷效果表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(ITEM_FLING_EFFECT);
    return super.saveEntityData(
        namedApiResources.results(),
        this::convertToItemFlingEffect,
        itemFlingEffectRepository,
        ItemFlingEffect::getName);
  }

  private ItemFlingEffect convertToItemFlingEffect(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.item.ItemFlingEffect apiResult =
        pokeApiFactory.getSingleResource(
            ITEM_FLING_EFFECT, NamedApiResourceUtils.getId(namedApiResource));
    ItemFlingEffect itemFlingEffect = new ItemFlingEffect();
    itemFlingEffect.setId(apiResult.id());
    itemFlingEffect.setInternalName(apiResult.name());
    itemFlingEffect.setName(apiResult.name());
    LocalizationUtils.getLocalizationEffect(apiResult.effectEntries())
        .ifPresent(
            effect -> {
              itemFlingEffect.setEffect(effect.effect());
            });
    return itemFlingEffect;
  }
}
