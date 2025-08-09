package io.github.lishangbu.avalon.shell.dataset.component;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiDataTypeEnum;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;

/**
 * 基础数据类型自动补全
 *
 * @author lishangbu
 * @since 2025/8/9
 */
public class PokeApiDataTypeNameProvider implements ValueProvider {
  @Override
  public List<CompletionProposal> complete(CompletionContext context) {
    String input = context.currentWord();
    return Arrays.stream(PokeApiDataTypeEnum.values())
        .map(PokeApiDataTypeEnum::getType)
        .map(CompletionProposal::new)
        .filter(proposal -> proposal.value().startsWith(input)) // 根据用户输入过滤
        .collect(Collectors.toList());
  }
}
