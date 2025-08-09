package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.TYPE;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.TypeRelations;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;
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
  private final PokeApiFactory pokeApiFactory;

  private final TypeRepository typeRepository;

  private final TypeDamageRelationRepository typeDamageRelationRepository;

  public TypeRelationDataSetShellComponent(
      PokeApiFactory pokeApiFactory,
      TypeRepository typeRepository,
      TypeDamageRelationRepository typeDamageRelationRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.typeRepository = typeRepository;
    this.typeDamageRelationRepository = typeDamageRelationRepository;
  }

  @ShellMethod(key = "dataset refresh typeDamageRelation", value = "刷新数据库中的属性克制关系表数据")
  @Transactional(rollbackFor = Exception.class)
  public String refreshTypeDamageRelation() {

    // 处理所有Type的刷新
    log.debug("没有名字输入, 将更新属性表中对应的所有伤害关系");
    List<Type> types = typeRepository.findAll();
    if (types.isEmpty()) {
      throw new RuntimeException("没有找到任何属性数据,请先完成数据库属性表的初始化工作");
    } else {
      log.debug("共找到{}个属性数据, 开始逐个更新", types.size());
      types.forEach(type -> refreshTypeDamageRelationById(type.getId()));
    }

    return "处理完成";
  }

  /**
   * 刷新指数据库中指定属性的伤害关系
   *
   * @param id 属性ID
   */
  private void refreshTypeDamageRelationById(Integer id) {
    Optional<Type> typeOptional = typeRepository.findById(id);
    if (typeOptional.isEmpty()) {
      throw new RuntimeException(String.format("名称为%s的数据在属性表中无法找到,请检查输入是否有误", id));
    }

    Type currentType = typeOptional.get();
    io.github.lishangbu.avalon.pokeapi.model.pokemon.Type type =
        pokeApiFactory.getSingleResource(TYPE, id);
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
      List<NamedApiResource<io.github.lishangbu.avalon.pokeapi.model.pokemon.Type>> typeResources,
      Type currentType,
      List<TypeDamageRelation> typeDamageRelations,
      String relationType) {
    if (CollectionUtils.isEmpty(typeResources)) {
      return;
    }

    for (NamedApiResource<io.github.lishangbu.avalon.pokeapi.model.pokemon.Type>
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
