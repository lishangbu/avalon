package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.EvolutionChainExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.evolution.ChainLink;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionChain;
import io.github.lishangbu.avalon.pokeapi.model.evolution.EvolutionDetail;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 进化链数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class EvolutionChainDataProvider implements PokeApiDataProvider<EvolutionChainExcelDTO> {
    @Autowired protected PokeApiService pokeApiService;

    @Override
    public List<EvolutionChainExcelDTO> fetch(
            PokeDataTypeEnum typeEnum, Class<EvolutionChainExcelDTO> type) {
        NamedAPIResourceList namedAPIResourceList =
                pokeApiService.listNamedAPIResources(PokeDataTypeEnum.EVOLUTION_CHAIN);
        List<EvolutionChainExcelDTO> result = new ArrayList<>();
        namedAPIResourceList
                .results()
                .forEach(
                        namedApiResource -> {
                            EvolutionChain evolutionChain =
                                    pokeApiService.getEntityFromUri(
                                            PokeDataTypeEnum.EVOLUTION_CHAIN,
                                            NamedApiResourceUtils.getId(namedApiResource));
                            result.addAll(processChain(evolutionChain.chain(), null));
                        });
        return result;
    }

    /**
     * 根据进化细节解析并构建进化链Excel DTO对象
     *
     * @param fromPokemonSpeciesId 进化前的宝可梦种类ID
     * @param toPokemonSpeciesId   进化后的宝可梦种类ID
     * @param evolutionDetail      进化细节对象
     * @return 构建的进化链Excel DTO对象
     */
    private EvolutionChainExcelDTO resolveEvolutionChainExcelDTO(
            Integer fromPokemonSpeciesId,
            Integer toPokemonSpeciesId,
            EvolutionDetail evolutionDetail) {
        EvolutionChainExcelDTO chainExcelDTO = new EvolutionChainExcelDTO();
        chainExcelDTO.setFromPokemonSpeciesId(fromPokemonSpeciesId);
        chainExcelDTO.setToPokemonSpeciesId(toPokemonSpeciesId);
        chainExcelDTO.setBaseFormId(NamedApiResourceUtils.getId(evolutionDetail.baseForm()));
        chainExcelDTO.setGender(evolutionDetail.gender());
        chainExcelDTO.setHeldItemId(NamedApiResourceUtils.getId(evolutionDetail.heldItem()));
        chainExcelDTO.setItemId(NamedApiResourceUtils.getId(evolutionDetail.item()));
        chainExcelDTO.setKnownMoveId(NamedApiResourceUtils.getId(evolutionDetail.knownMove()));
        chainExcelDTO.setKnownMoveTypeId(
                NamedApiResourceUtils.getId(evolutionDetail.knownMoveType()));
        chainExcelDTO.setLocationId(NamedApiResourceUtils.getId(evolutionDetail.location()));
        chainExcelDTO.setMinAffection(evolutionDetail.minAffection());
        chainExcelDTO.setMinBeauty(evolutionDetail.minBeauty());
        chainExcelDTO.setMinDamageTaken(evolutionDetail.minDamageTaken());
        chainExcelDTO.setMinHappiness(evolutionDetail.minHappiness());
        chainExcelDTO.setMinLevel(evolutionDetail.minLevel());
        chainExcelDTO.setMinMoveCount(evolutionDetail.minMoveCount());
        chainExcelDTO.setMinSteps(evolutionDetail.minSteps());
        chainExcelDTO.setNeedsMultiplayer(evolutionDetail.needsMultiplayer());
        chainExcelDTO.setNeedsOverworldRain(evolutionDetail.needsOverworldRain());
        chainExcelDTO.setPartySpeciesId(
                NamedApiResourceUtils.getId(evolutionDetail.partySpecies()));
        chainExcelDTO.setPartyTypeId(NamedApiResourceUtils.getId(evolutionDetail.partyType()));
        chainExcelDTO.setRegionId(NamedApiResourceUtils.getId(evolutionDetail.region()));
        chainExcelDTO.setRelativePhysicalStats(evolutionDetail.relativePhysicalStats());
        chainExcelDTO.setTimeOfDay(evolutionDetail.timeOfDay());
        chainExcelDTO.setTradeSpeciesId(
                NamedApiResourceUtils.getId(evolutionDetail.tradeSpecies()));
        chainExcelDTO.setTriggerId(NamedApiResourceUtils.getId(evolutionDetail.trigger()));
        chainExcelDTO.setTurnUpsideDown(evolutionDetail.turnUpsideDown());
        chainExcelDTO.setUsedMoveId(NamedApiResourceUtils.getId(evolutionDetail.usedMove()));
        return chainExcelDTO;
    }

    /**
     * 递归处理进化链，生成进化链Excel DTO列表
     *
     * @param chainLink     当前链链接
     * @param fromSpeciesId 进化前物种ID
     * @return 进化链Excel DTO列表
     */
    private List<EvolutionChainExcelDTO> processChain(ChainLink chainLink, Integer fromSpeciesId) {
        List<EvolutionChainExcelDTO> dtos = new ArrayList<>();
        Integer currentSpeciesId = NamedApiResourceUtils.getId(chainLink.species());
        if (fromSpeciesId != null) {
            for (EvolutionDetail detail : chainLink.evolutionDetails()) {
                EvolutionChainExcelDTO dto =
                        resolveEvolutionChainExcelDTO(fromSpeciesId, currentSpeciesId, detail);
                dtos.add(dto);
            }
        }
        for (ChainLink subLink : chainLink.evolvesTo()) {
            dtos.addAll(processChain(subLink, currentSpeciesId));
        }
        return dtos;
    }
}
