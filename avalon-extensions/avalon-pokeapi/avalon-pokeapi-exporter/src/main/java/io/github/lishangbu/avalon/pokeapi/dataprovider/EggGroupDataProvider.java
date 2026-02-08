package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.EggGroupExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.EggGroup;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/// 蛋组数据提供者
///
/// @author lishangbu
/// @since 2026/2/4
@Service
public class EggGroupDataProvider extends AbstractPokeApiDataProvider<EggGroup, EggGroupExcelDTO>
    implements InitializingBean {
  private Map<String, String> EGG_GROUP_TEXT_MAPPING;
  private Map<String, String> EGG_GROUP_CHARACTERISTICS_MAPPING;

  @Override
  public EggGroupExcelDTO convert(EggGroup eggGroup) {
    EggGroupExcelDTO result = new EggGroupExcelDTO();
    result.setId(eggGroup.id());
    result.setInternalName(eggGroup.name());
    result.setName(resolveLocalizedNameFromNames(eggGroup.names(), eggGroup.name()));
    result.setText(EGG_GROUP_TEXT_MAPPING.get(result.getName()));
    result.setCharacteristics(EGG_GROUP_CHARACTERISTICS_MAPPING.get(result.getName()));
    return result;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    EGG_GROUP_TEXT_MAPPING = new ConcurrentHashMap<>(16);
    EGG_GROUP_TEXT_MAPPING.put("怪兽", "像是怪兽一样，或者比较野性。");
    EGG_GROUP_TEXT_MAPPING.put("水中1", "可以两栖或多栖。");
    EGG_GROUP_TEXT_MAPPING.put("虫", "外表长得像虫子。");
    EGG_GROUP_TEXT_MAPPING.put("飞行", "外表长得像鸟、蝙蝠等会飞行的生物。");
    EGG_GROUP_TEXT_MAPPING.put("陆上", "最大的蛋群，住在陆地上的宝可梦基本都属于这个群。");
    EGG_GROUP_TEXT_MAPPING.put("妖精", "外表可爱或具有传说灵异性质的生物。");
    EGG_GROUP_TEXT_MAPPING.put("植物", "外表长得像植物。");
    EGG_GROUP_TEXT_MAPPING.put("人型", "两足行走。");
    EGG_GROUP_TEXT_MAPPING.put("水中３", "水中无脊椎动物。");
    EGG_GROUP_TEXT_MAPPING.put("矿物", "结晶或硅基生物。");
    EGG_GROUP_TEXT_MAPPING.put("不定形", "没有固定外表。");
    EGG_GROUP_TEXT_MAPPING.put("水中2", "像是鱼一类的脊椎动物。");
    EGG_GROUP_TEXT_MAPPING.put("百变怪", "顾名思义，百变怪是这个群中唯一的宝可梦，可以和除了未发现群及百变怪群以外的所有宝可梦生蛋（无视性别）。");
    EGG_GROUP_TEXT_MAPPING.put("龙", "外表长得像龙或者具有龙的特质的宝可梦。");
    EGG_GROUP_TEXT_MAPPING.put("未发现", "不能和任何宝可梦生蛋。");

    EGG_GROUP_CHARACTERISTICS_MAPPING = new ConcurrentHashMap<>(16);
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("怪兽", "这个蛋群的宝可梦大多原型基于特摄影片中的怪兽以及爬行动物。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("水中1", "这个蛋群的宝可梦大多原型基于两栖动物和水边生活的多栖动物。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("虫", "这个蛋群的宝可梦大多原型基于昆虫和节肢动物。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("飞行", "这个蛋群的宝可梦原型大多基于鸟类、蝙蝠、会飞的爬行动物甚至是神话中会飞的小妖精。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("陆上", "这个蛋群的宝可梦大多原型基于哺乳动物和爬行动物，以及翅膀退化的鸟类。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("妖精", "这个蛋群的宝可梦大多原型基于可爱的小型动物和神话中的妖精。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("植物", "这个蛋群的宝可梦大多原型基于植物和真菌，以及身上长有植物或真菌的动物。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("人型", "这个蛋群的宝可梦都是直立行走的人型生物。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("水中３", "这个蛋群的宝可梦大多原型基于非鱼类的深海水生动物。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("矿物", "这个蛋群的宝可梦大多原型基于无机物和身上带有无机物的生物。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("不定形", "这个蛋群的宝可梦大多原型基于软体动物、灵体，以及身体柔软的生物或非生物。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("水中2", "这个蛋群的宝可梦大多原型基于鱼类，乌贼以及章鱼。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put(
        "百变怪", "这个蛋群只有百变怪。处于这个蛋群的宝可梦可以与除未发现群和百变怪群外的任何蛋群的宝可梦生蛋，蛋的种类必然是另一方。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("龙", "这个蛋群的宝可梦大多原型基于传说中的龙以及与龙有关的动物（蜥蜴、海马等）。");
    EGG_GROUP_CHARACTERISTICS_MAPPING.put("未发现", "属于此蛋群的宝可梦都无法生蛋。");
  }
}
