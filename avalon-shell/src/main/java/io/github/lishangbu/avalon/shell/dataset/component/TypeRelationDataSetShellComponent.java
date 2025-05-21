package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.TypeRelations;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiTemplate;
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
  private final PokeApiTemplate pokeApiTemplate;

  private final TypeRepository typeRepository;

  private final TypeDamageRelationRepository typeDamageRelationRepository;

  public TypeRelationDataSetShellComponent(
      PokeApiTemplate pokeApiTemplate,
      TypeRepository typeRepository,
      TypeDamageRelationRepository typeDamageRelationRepository) {
    this.pokeApiTemplate = pokeApiTemplate;
    this.typeRepository = typeRepository;
    this.typeDamageRelationRepository = typeDamageRelationRepository;
  }

  @ShellMethod(key = "dataset refresh type", value = "刷新数据库中的TYPE表数据")
  public String refreshType(
      @ShellOption(help = "每页偏移量", defaultValue = "0") Integer offset,
      @ShellOption(help = "每页数量", defaultValue = "100") Integer limit) {
    NamedAPIResourceList namedApiResources = pokeApiTemplate.listTypes(offset, limit);
    List<io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type> loadedTypes =
        new ArrayList<>();
    for (NamedApiResource namedApiResource : namedApiResources.results()) {
      io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type result =
          pokeApiTemplate.getType(namedApiResource.name());
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

  @ShellMethod(
      key = "dataset refresh type damage relation",
      value = "刷新数据库中的TYPE_DAMAGE_RELATION表数据")
  public String refreshTypeDamageRelation(
      @ShellOption(help = "属性内部名称", defaultValue = "") String name) {
    if (!StringUtils.hasText(name)) {
      log.info("没有名字输入,将更新Type表中对应的所有伤害关系");
      for (Type type : typeRepository.findAll()) {
        refreshTypeDamageRelationByName(type.getInternalName());
      }
    } else {
      refreshTypeDamageRelationByName(name);
    }
    return "处理完成";
  }

  private void refreshTypeDamageRelationByName(String name) {
    Optional<Type> typeOptional = typeRepository.findByInternalName(name);
    if (typeOptional.isEmpty()) {
      log.warn("名称为{}的数据在数据库中无法找到,请检查输入是否有误，或者先提前初始化TYPE数据库中的数据", name);
      return;
    }

    Type currentType = typeOptional.get();
    io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type type = pokeApiTemplate.getType(name);
    TypeRelations typeRelations = type.damageRelations();
    List<TypeDamageRelation> typeDamageRelations = new ArrayList<>();

    processTypeRelations(
        typeRelations.noDamageFrom(), currentType, typeDamageRelations, "noDamageFrom");
    processTypeRelations(
        typeRelations.noDamageTo(), currentType, typeDamageRelations, "noDamageTo");
    processTypeRelations(
        typeRelations.doubleDamageFrom(), currentType, typeDamageRelations, "doubleDamageFrom");
    processTypeRelations(
        typeRelations.doubleDamageTo(), currentType, typeDamageRelations, "doubleDamageTo");
    processTypeRelations(
        typeRelations.halfDamageFrom(), currentType, typeDamageRelations, "halfDamageFrom");
    processTypeRelations(
        typeRelations.halfDamageTo(), currentType, typeDamageRelations, "halfDamageTo");

    // Save or process `typeDamageRelations` as needed
    for (TypeDamageRelation typeDamageRelation : typeDamageRelations) {
      typeDamageRelationRepository
          .findTypeDamageRelationByAttackerTypeAndDefenderType(
              typeDamageRelation.getAttackerType(), typeDamageRelation.getDefenderType())
          .ifPresentOrElse(
              dbData -> {
                dbData.setDamageRate(typeDamageRelation.getDamageRate());
                typeDamageRelationRepository.saveAndFlush(typeDamageRelation);
              },
              () -> {
                typeDamageRelationRepository.saveAndFlush(typeDamageRelation);
              });
    }
  }

  private void processTypeRelations(
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
