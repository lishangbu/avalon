package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation.TypeDamageRelationId;
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository;
import io.github.lishangbu.avalon.dataset.service.TypeDamageRelationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// 属性克制关系服务实现。
@Service
@RequiredArgsConstructor
public class TypeDamageRelationServiceImpl implements TypeDamageRelationService {
    private final TypeDamageRelationRepository typeDamageRelationRepository;

    @Override
    public Page<TypeDamageRelation> getPageByCondition(
            TypeDamageRelation condition, Pageable pageable) {
        TypeDamageRelation probe = condition == null ? new TypeDamageRelation() : condition;
        return typeDamageRelationRepository.findAll(
                Example.of(probe, ExampleMatcher.matching().withIgnoreNullValues()), pageable);
    }

    @Override
    public TypeDamageRelation save(TypeDamageRelation relation) {
        return typeDamageRelationRepository.save(relation);
    }

    @Override
    public TypeDamageRelation update(TypeDamageRelation relation) {
        return typeDamageRelationRepository.save(relation);
    }

    @Override
    public void removeById(TypeDamageRelationId id) {
        typeDamageRelationRepository.deleteById(id);
    }

    @Override
    public List<TypeDamageRelation> listByCondition(TypeDamageRelation condition) {
        TypeDamageRelation probe = condition == null ? new TypeDamageRelation() : condition;
        return typeDamageRelationRepository.findAll(
                Example.of(probe, ExampleMatcher.matching().withIgnoreNullValues()));
    }
}
