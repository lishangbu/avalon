package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.ItemAttributeRelation;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 物品属性
 *
 * @author lishangbu
 * @since 2025/8/10
 */
@ShellComponent
public class ItemAttributeRelationShellComponent {
  private final PokeApiService pokeApiService;

  private final JdbcAggregateTemplate jdbcAggregateTemplate;

  public ItemAttributeRelationShellComponent(
      PokeApiService pokeApiService, JdbcAggregateTemplate jdbcAggregateTemplate) {
    this.pokeApiService = pokeApiService;
    this.jdbcAggregateTemplate = jdbcAggregateTemplate;
  }

  @ShellMethod(key = "dataset generate itemAttributeRelation", value = "生成并持久化物品属性关系")
  @Transactional(rollbackFor = Exception.class)
  public String generateAndPersistItemAttributeRelation() {
    // 物品属性的处理逻辑
    NamedAPIResourceList itemNamedApiResources =
        pokeApiService.listNamedAPIResources(PokeApiDataTypeEnum.ITEM);
    List<ItemAttributeRelation> itemAttributeRelations = new ArrayList<>();
    for (NamedApiResource result : itemNamedApiResources.results()) {
      Object singleResource =
          pokeApiService.getEntityFromUri(
              PokeApiDataTypeEnum.ITEM, NamedApiResourceUtils.getId(result));
      if (singleResource instanceof io.github.lishangbu.avalon.pokeapi.model.item.Item itemData) {
        if (itemData.attributes() != null && !itemData.attributes().isEmpty()) {
          for (NamedApiResource attribute : itemData.attributes()) {
            ItemAttributeRelation itemAttributeRelation = new ItemAttributeRelation();
            itemAttributeRelation.setItemId(itemData.id());
            itemAttributeRelation.setItemAttributeId(NamedApiResourceUtils.getId(attribute));
            itemAttributeRelations.add(itemAttributeRelation);
          }
        }
      }
    }
    jdbcAggregateTemplate.deleteAll(ItemAttributeRelation.class);
    jdbcAggregateTemplate.insertAll(itemAttributeRelations);
    return "生成并持久化物品属性关系成功";
  }
}
