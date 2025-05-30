package io.github.lishangbu.avalon.pokeapi.model.evolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 进化链中的一个环节，描述了宝可梦进化的详细信息
 *
 * @param isBaby 此链接是否适用于幼年宝可梦。这只会在基础链接上为真
 * @param species 进化链此点的宝可梦物种
 * @param evolutionDetails 有关参考宝可梦物种进化的具体细节{@link EvolutionDetail}
 * @param evolvesTo 链接对象列表，表示可能的进化方向{@link ChainLink}
 * @author lishangbu
 * @see EvolutionDetail
 * @see ChainLink
 * @since 2025/5/24
 */
public record ChainLink(
    @JsonProperty("is_baby") Boolean isBaby,
    NamedApiResource<?> species,
    @JsonProperty("evolution_details") List<EvolutionDetail> evolutionDetails,
    @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {}
