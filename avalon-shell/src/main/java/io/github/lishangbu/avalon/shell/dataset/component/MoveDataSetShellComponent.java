package io.github.lishangbu.avalon.shell.dataset.component;

import static io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum.MOVE;

import io.github.lishangbu.avalon.dataset.entity.Move;
import io.github.lishangbu.avalon.dataset.repository.*;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招式数据处理命令
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ShellComponent
public class MoveDataSetShellComponent extends AbstractDataSetShellComponent {
  private final PokeApiFactory pokeApiFactory;
  private final MoveRepository moveRepository;
  private final MoveAilmentRepository moveAilmentRepository;
  private final MoveCategoryRepository moveCategoryRepository;
  private final MoveDamageClassRepository moveDamageClassRepository;
  private final MoveTargetRepository moveTargetRepository;
  private final TypeRepository typeRepository;

  public MoveDataSetShellComponent(
      PokeApiFactory pokeApiFactory,
      MoveRepository moveRepository,
      MoveAilmentRepository moveAilmentRepository,
      MoveCategoryRepository moveCategoryRepository,
      MoveDamageClassRepository moveDamageClassRepository,
      MoveTargetRepository moveTargetRepository,
      TypeRepository typeRepository) {
    this.pokeApiFactory = pokeApiFactory;
    this.moveRepository = moveRepository;
    this.moveAilmentRepository = moveAilmentRepository;
    this.moveCategoryRepository = moveCategoryRepository;
    this.moveDamageClassRepository = moveDamageClassRepository;
    this.moveTargetRepository = moveTargetRepository;
    this.typeRepository = typeRepository;
  }

  @ShellMethod(key = "dataset refresh move", value = "刷新数据库中的招式表数据")
  @Override
  @Transactional(rollbackFor = Exception.class)
  public String refreshData() {
    NamedAPIResourceList namedApiResources = pokeApiFactory.getPagedResource(MOVE);
    return super.saveEntityData(
        namedApiResources.results(), this::convertToMove, moveRepository, Move::getName);
  }

  private Move convertToMove(NamedApiResource namedApiResource) {
    io.github.lishangbu.avalon.pokeapi.model.move.Move apiResult =
        pokeApiFactory.getSingleResource(MOVE, NamedApiResourceUtils.getId(namedApiResource));
    Move move = new Move();
    move.setId(apiResult.id());
    move.setInternalName(apiResult.name());
    LocalizationUtils.getLocalizationName(apiResult.names())
        .ifPresent(
            name -> {
              move.setName(name.name());
            });
    move.setAccuracy(apiResult.accuracy());
    moveDamageClassRepository
        .findByInternalName(apiResult.damageClass().name())
        .ifPresent(move::setDamageClass);

    move.setEffectChance(apiResult.effectChance());
    LocalizationUtils.getLocalizationVerboseEffect(apiResult.effectEntries())
        .ifPresent(
            verboseEffect -> {
              move.setEffect(verboseEffect.effect());
              move.setShortEffect(verboseEffect.shortEffect());
            });
    move.setPower(apiResult.power());
    move.setPp(apiResult.pp());
    move.setPriority(apiResult.priority());
    moveTargetRepository.findByInternalName(apiResult.target().name()).ifPresent(move::setTarget);
    LocalizationUtils.getLocalizationMoveFlavorText(apiResult.flavorTextEntries())
        .ifPresent(moveFlavorText -> move.setText(moveFlavorText.flavorText()));
    typeRepository.findByInternalName(apiResult.type().name()).ifPresent(move::setType);
    io.github.lishangbu.avalon.pokeapi.model.move.MoveMetaData metaApiResult = apiResult.meta();
    if (metaApiResult != null) {
      moveAilmentRepository
          .findByInternalName(metaApiResult.ailment().name())
          .ifPresent(move::setAilment);
      moveCategoryRepository
          .findByInternalName(metaApiResult.category().name())
          .ifPresent(move::setCategory);
      move.setAilmentChance(metaApiResult.ailmentChance());
      move.setDrain(metaApiResult.drain());
      move.setHealing(metaApiResult.healing());
      move.setCritRate(metaApiResult.critRate());
      move.setFlinchChance(metaApiResult.flinchChance());
      move.setMinHits(metaApiResult.minHits());
      move.setMaxTurns(metaApiResult.maxTurns());
    }
    return move;
  }
}
