package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.TypeRelations;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class TypeRelationDataSetShellComponent extends AbstractDataSetShellComponent {

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

  @Override
  @ShellMethod(key = "dataset refresh typeDamageRelation", value = "刷新数据库中的属性克制关系表数据")
  public String refreshData(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources = pokeApiService.listTypes(offset, limit);
    return super.saveMultipleEntityData(
        namedApiResources.results(),
        this::convertToTypeDamageRelation,
        typeDamageRelationRepository,
        typedamageRelation ->
            String.format(
                "%s对%s:%s",
                typedamageRelation.getAttackerType().getName(),
                typedamageRelation.getDefenderType().getName(),
                getDamageRateDescription(typedamageRelation.getDamageRate())));
  }

  private List<TypeDamageRelation> convertToTypeDamageRelation(NamedApiResource namedApiResource) {

    io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type result =
        pokeApiService.getType(namedApiResource.name());

    Type currentType = getType(result.name());
    TypeRelations typeRelations = result.damageRelations();
    // 创建一个集合来存储所有的 TypeDamageRelation
    List<TypeDamageRelation> typeDamageRelations = new ArrayList<>();
    // 使用一个方法来避免重复的代码
    calculateAllDamageRelations(typeRelations, currentType, typeDamageRelations);
    // 使用集合来批量处理保存
    mergeTypeDamageRelationIds(typeDamageRelations);
    return typeDamageRelations;
  }

  private Type getType(String internalName) {
    Optional<Type> typeOptional = typeRepository.findByInternalName(internalName);
    if (typeOptional.isEmpty()) {
      throw new RuntimeException("名称为" + internalName + "的数据在数据库中无法找到,请提前初始化属性表中的数据");
    }
    return typeOptional.get();
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
   * 合并数据库中已有记录的ID
   *
   * @param typeDamageRelations
   */
  private void mergeTypeDamageRelationIds(List<TypeDamageRelation> typeDamageRelations) {
    for (TypeDamageRelation typeDamageRelation : typeDamageRelations) {
      Optional<TypeDamageRelation> existingRelationOpt =
          typeDamageRelationRepository.findTypeDamageRelationByAttackerTypeAndDefenderType(
              typeDamageRelation.getAttackerType(), typeDamageRelation.getDefenderType());
      // 数据存在时，写入ID，以便ORM走更新逻辑
      existingRelationOpt.ifPresent(
          damageRelation -> typeDamageRelation.setId(damageRelation.getId()));
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

  private String getDamageRateDescription(Float damageRate) {
    if (damageRate == 0.0f) {
      return "没有效果";
    } else if (damageRate == 0.5f) {
      return "效果不好";
    } else if (damageRate == 2.0f) {
      return "效果绝佳";
    } else {
      return "错误效果,请检查伤害系数加成";
    }
  }
}
