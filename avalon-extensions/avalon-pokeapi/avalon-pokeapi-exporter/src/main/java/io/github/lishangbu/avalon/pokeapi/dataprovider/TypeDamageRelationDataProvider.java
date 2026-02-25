package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.TypeDamageRelationExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.TypeRelations;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 属性相互克制关系数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class TypeDamageRelationDataProvider
        implements PokeApiDataProvider<TypeDamageRelationExcelDTO> {
    @Autowired protected PokeApiService pokeApiService;

    /// 获取属性相互克制关系数据
    ///
    /// @param typeEnum                   数据类型枚举
    /// @param typeDamageRelationExcelDTO DTO类
    /// @return 属性相互克制关系DTO列表
    @Override
    public List<TypeDamageRelationExcelDTO> fetch(
            PokeDataTypeEnum typeEnum,
            Class<TypeDamageRelationExcelDTO> typeDamageRelationExcelDTO) {
        Map<Map<Integer, Integer>, Float> typeDamageRelationCache = new HashMap<>();
        NamedAPIResourceList namedAPIResourceList =
                pokeApiService.listNamedAPIResources(PokeDataTypeEnum.TYPE);
        List<Type> types =
                namedAPIResourceList.results().stream()
                        .map(
                                namedApiResource ->
                                        (Type)
                                                pokeApiService.getEntityFromUri(
                                                        PokeDataTypeEnum.TYPE,
                                                        NamedApiResourceUtils.getId(
                                                                namedApiResource)))
                        .toList();
        for (Type type : types) {
            TypeRelations typeRelations = type.damageRelations();
            addDamageFrom(
                    typeDamageRelationCache, typeRelations.doubleDamageFrom(), type.id(), 2.0f);
            addDamageFrom(typeDamageRelationCache, typeRelations.halfDamageFrom(), type.id(), 0.5f);
            addDamageFrom(typeDamageRelationCache, typeRelations.noDamageFrom(), type.id(), 0.0f);
            addDamageTo(typeDamageRelationCache, typeRelations.doubleDamageTo(), type.id(), 2.0f);
            addDamageTo(typeDamageRelationCache, typeRelations.halfDamageTo(), type.id(), 0.5f);
            addDamageTo(typeDamageRelationCache, typeRelations.noDamageTo(), type.id(), 0.0f);
        }

        // 补全1-18 ID范围内的伤害关系，如果不存在则设为1倍
        for (int attackerId = 1; attackerId <= 18; attackerId++) {
            for (int defenderId = 1; defenderId <= 18; defenderId++) {
                Map<Integer, Integer> key = Map.of(attackerId, defenderId);
                typeDamageRelationCache.putIfAbsent(key, 1.0f);
            }
        }

        return typeDamageRelationCache.entrySet().stream()
                .map(
                        entry -> {
                            Map<Integer, Integer> key = entry.getKey();
                            Integer attackerId = key.keySet().iterator().next();
                            Integer defenderId = key.values().iterator().next();
                            return new TypeDamageRelationExcelDTO(
                                    attackerId, defenderId, entry.getValue());
                        })
                .sorted(
                        Comparator.comparing(TypeDamageRelationExcelDTO::getAttackingTypeId)
                                .thenComparing(TypeDamageRelationExcelDTO::getDefendingTypeId))
                .collect(Collectors.toList());
    }

    /// 添加来自攻击方的伤害关系
    ///
    /// @param cache      缓存映射
    /// @param resources  资源列表
    /// @param defenderId 防御方ID
    /// @param multiplier 伤害倍数
    private void addDamageFrom(
            Map<Map<Integer, Integer>, Float> cache,
            List<NamedApiResource<Type>> resources,
            int defenderId,
            float multiplier) {
        for (NamedApiResource<Type> resource : resources) {
            Integer attackerId = NamedApiResourceUtils.getId(resource);
            if (attackerId != null) {
                Map<Integer, Integer> key = Map.of(attackerId, defenderId);
                if (!cache.containsKey(key)) {
                    cache.put(key, multiplier);
                }
            }
        }
    }

    /// 添加给防御方的伤害关系
    ///
    /// @param cache      缓存映射
    /// @param resources  资源列表
    /// @param attackerId 攻击方ID
    /// @param multiplier 伤害倍数
    private void addDamageTo(
            Map<Map<Integer, Integer>, Float> cache,
            List<NamedApiResource<Type>> resources,
            int attackerId,
            float multiplier) {
        for (NamedApiResource<Type> resource : resources) {
            Integer defenderId = NamedApiResourceUtils.getId(resource);
            if (defenderId != null) {
                Map<Integer, Integer> key = Map.of(attackerId, defenderId);
                if (!cache.containsKey(key)) {
                    cache.put(key, multiplier);
                }
            }
        }
    }
}
