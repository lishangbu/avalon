package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.MoveExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.move.Move;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveMetaData;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 招式数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class MoveDataProvider extends AbstractPokeApiDataProvider<Move, MoveExcelDTO> {

    @Override
    public MoveExcelDTO convert(Move move) {
        MoveExcelDTO result = new MoveExcelDTO();
        result.setId(move.id());
        result.setInternalName(move.name());
        result.setName(resolveLocalizedName(move.names(), move.name()));
        result.setAccuracy(move.accuracy());
        result.setEffectChance(move.effectChance());
        result.setPp(move.pp());
        result.setPriority(move.priority());
        result.setPower(move.power());
        LocalizationUtils.getLocalizationMoveFlavorText(move.flavorTextEntries())
                .ifPresent(flavorTextEntry -> result.setText(flavorTextEntry.flavorText()));
        LocalizationUtils.getLocalizationVerboseEffect(move.effectEntries())
                .ifPresent(
                        verboseEffect -> {
                            result.setShortEffect(verboseEffect.shortEffect());
                            result.setEffect(verboseEffect.effect());
                        });
        final MoveMetaData meta = move.meta();
        if (meta != null) {
            result.setMinHits(meta.minHits());
            result.setMaxHits(meta.maxHits());
            result.setMinTurns(meta.minTurns());
            result.setMaxTurns(meta.maxTurns());
            result.setDrain(meta.drain());
            result.setMaxTurns(meta.maxTurns());
            result.setHealing(meta.healing());
            result.setCritRate(meta.critRate());
            result.setAilmentChance(meta.ailmentChance());
            result.setFlinchChance(meta.flinchChance());
            result.setStatChance(meta.statChance());
            result.setMoveAilmentId(NamedApiResourceUtils.getId(meta.ailment()));
            result.setMoveCategoryId(NamedApiResourceUtils.getId(meta.category()));
        }
        result.setMoveTargetId(NamedApiResourceUtils.getId(move.target()));
        result.setTypeId(NamedApiResourceUtils.getId(move.type()));
        result.setMoveDamageClassId(NamedApiResourceUtils.getId(move.damageClass()));
        return result;
    }
}
