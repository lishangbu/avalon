package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.TypeRelations;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 属性相关数据集处理命令
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@ShellComponent
@ShellCommandGroup("数据集处理")
public class TypeRelationDataSetShellComponent {

  private static final Logger log =
      LoggerFactory.getLogger(TypeRelationDataSetShellComponent.class);
  private final PokeApiService pokeApiService;

  private final TypeRepository typeRepository;

  private final TypeDamageRelationRepository typeDamageRelationRepository;

  public TypeRelationDataSetShellComponent(
      PokeApiService pokeApiService,
      TypeRepository typeRepository,
      TypeDamageRelationRepository typeDamageRelationRepository) {
    this.pokeApiService = pokeApiService;
    this.typeRepository = typeRepository;
    this.typeDamageRelationRepository = typeDamageRelationRepository;
  }

  @ShellMethod(key = "dataset refresh type", value = "刷新数据库中的属性表数据")
  public String refreshType(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources = pokeApiService.listTypes(offset, limit);
    List<io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type> loadedTypes =
        new ArrayList<>();
    for (NamedApiResource namedApiResource : namedApiResources.results()) {
      io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type result =
          pokeApiService.getType(namedApiResource.name());
      loadedTypes.add(result);
      Type type = new Type();
      type.setId(result.id());
      type.setInternalName(result.name());
      // 星晶属性没有简体中文，所以特殊处理一下多取一个繁体中文
      Optional<Name> localizationName =
          LocalizationUtils.getLocalizationName(
              result.names(),
              LocalizationUtils.SIMPLIFIED_CHINESE,
              LocalizationUtils.TRADITIONAL_CHINESE);
      type.setName(
          localizationName.isPresent() ? localizationName.get().name() : type.getInternalName());
      typeRepository.save(type);
    }

    return String.format(
        "数据处理完成，本次处理从API接口获取了数据:%s。\r\n当前共有数据:%d条",
        loadedTypes.stream()
            .map(io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type::name)
            .collect(Collectors.joining(",")),
        typeRepository.count());
  }

  @ShellMethod(key = "dataset refresh typeDamageRelation", value = "刷新数据库中的属性克制关系表数据")
  public String refreshTypeDamageRelation(
      @ShellOption(help = "属性内部名称", defaultValue = "") String name) {

    if (StringUtils.hasText(name)) {
      // 处理单个Type的刷新
      log.info("开始更新单个属性：{}", name);
      refreshTypeDamageRelationByName(name);
    } else {
      // 处理所有Type的刷新
      log.info("没有名字输入, 将更新属性表中对应的所有伤害关系");
      List<Type> types = typeRepository.findAll();
      if (types.isEmpty()) {
        log.warn("没有找到任何属性数据");
      } else {
        log.info("共找到{}个属性数据, 开始逐个更新", types.size());
        types.forEach(type -> refreshTypeDamageRelationByName(type.getInternalName()));
      }
    }

    return "处理完成";
  }

  /**
   * 刷新指数据库中指定属性的伤害关系
   *
   * @param name 属性名称
   */
  private void refreshTypeDamageRelationByName(String name) {
    Optional<Type> typeOptional = typeRepository.findByInternalName(name);
    if (typeOptional.isEmpty()) {
      log.warn("名称为{}的数据在数据库中无法找到,请检查输入是否有误，或者先提前初始化TYPE数据库中的数据", name);
      return;
    }

    Type currentType = typeOptional.get();
    io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type type = pokeApiService.getType(name);
    TypeRelations typeRelations = type.damageRelations();

    // 创建一个集合来存储所有的 TypeDamageRelation
    List<TypeDamageRelation> typeDamageRelations = new ArrayList<>();

    // 使用一个方法来避免重复的代码
    calculateAllDamageRelations(typeRelations, currentType, typeDamageRelations);

    // 使用集合来批量处理保存
    saveOrUpdateTypeDamageRelations(typeDamageRelations);
  }

  /**
   * 计算所有的攻击方与防御方的效果绝佳(2x)、效果不理想(0.5x)和没有效果(0x)的伤害关系
   *
   * @param typeRelations POKE API查询到的属性克制关系
   * @param currentType 当前属性
   * @param typeDamageRelations 存储所有的 属性克制关系的集合
   */
  private void calculateAllDamageRelations(
      TypeRelations typeRelations, Type currentType, List<TypeDamageRelation> typeDamageRelations) {
    calculateTypeRelations(
        typeRelations.noDamageFrom(), currentType, typeDamageRelations, "noDamageFrom");
    calculateTypeRelations(
        typeRelations.noDamageTo(), currentType, typeDamageRelations, "noDamageTo");
    calculateTypeRelations(
        typeRelations.doubleDamageFrom(), currentType, typeDamageRelations, "doubleDamageFrom");
    calculateTypeRelations(
        typeRelations.doubleDamageTo(), currentType, typeDamageRelations, "doubleDamageTo");
    calculateTypeRelations(
        typeRelations.halfDamageFrom(), currentType, typeDamageRelations, "halfDamageFrom");
    calculateTypeRelations(
        typeRelations.halfDamageTo(), currentType, typeDamageRelations, "halfDamageTo");
  }

  /**
   * 向数据库中批量更新或保存属性克制关系
   *
   * @param typeDamageRelations
   */
  private void saveOrUpdateTypeDamageRelations(List<TypeDamageRelation> typeDamageRelations) {
    for (TypeDamageRelation typeDamageRelation : typeDamageRelations) {
      Optional<TypeDamageRelation> existingRelationOpt =
          typeDamageRelationRepository.findTypeDamageRelationByAttackerTypeAndDefenderType(
              typeDamageRelation.getAttackerType(), typeDamageRelation.getDefenderType());

      existingRelationOpt.ifPresentOrElse(
          existingRelation -> {
            existingRelation.setDamageRate(typeDamageRelation.getDamageRate());
            typeDamageRelationRepository.saveAndFlush(existingRelation);
          },
          () -> typeDamageRelationRepository.saveAndFlush(typeDamageRelation));
    }
  }

  /**
   * 计算攻击方与防御方的效果绝佳(2x)、效果不理想(0.5x)和没有效果(0x)的伤害关系
   *
   * @param typeResources POKE API查询的属性结果
   * @param currentType 当前属性
   * @param typeDamageRelations 存储所有的 属性克制关系的集合
   * @param relationType 用于计算的属性关系
   */
  private void calculateTypeRelations(
      List<NamedApiResource<io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type>>
          typeResources,
      Type currentType,
      List<TypeDamageRelation> typeDamageRelations,
      String relationType) {
    if (CollectionUtils.isEmpty(typeResources)) {
      return;
    }

    for (NamedApiResource<io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type>
        typeNamedApiResource : typeResources) {
      Optional<Type> typeOptional = typeRepository.findByInternalName(typeNamedApiResource.name());
      if (typeOptional.isEmpty()) {
        log.warn(
            "属性{}不存在,{}的属性{}无法正常加载",
            typeNamedApiResource.name(),
            currentType.getInternalName(),
            relationType);
      } else {
        TypeDamageRelation typeDamageRelation = new TypeDamageRelation();
        if (StringUtils.endsWithIgnoreCase(relationType, "From")) {
          typeDamageRelation.setAttackerType(typeOptional.get());
          typeDamageRelation.setDefenderType(currentType);
        } else if (StringUtils.endsWithIgnoreCase(relationType, "To")) {
          typeDamageRelation.setAttackerType(currentType);
          typeDamageRelation.setDefenderType(typeOptional.get());
        } else {
          log.warn("非法的属性伤害关系类型:{},后缀只能为From或者To", relationType);
        }
        if (StringUtils.startsWithIgnoreCase(relationType, "no")) {
          typeDamageRelation.setDamageRate(0.0f);
        } else if (StringUtils.startsWithIgnoreCase(relationType, "half")) {
          typeDamageRelation.setDamageRate(0.5f);
        } else if (StringUtils.startsWithIgnoreCase(relationType, "double")) {
          typeDamageRelation.setDamageRate(2.0f);
        } else {
          log.warn("非法的属性伤害关系类型:{},前缀只能为no,half或者double", relationType);
        }
        typeDamageRelations.add(typeDamageRelation);
      }
    }
  }
}
