package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.Type_;
import io.github.lishangbu.avalon.dataset.repository.TypeRepository;
import io.github.lishangbu.avalon.dataset.service.TypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 属性服务实现。
@Service
@RequiredArgsConstructor
public class TypeServiceImpl implements TypeService {
    private final TypeRepository typeRepository;

    @Override
    public Page<Type> getPageByCondition(Type type, Pageable pageable) {
        return typeRepository.findAll(
                Example.of(
                        type,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        Type_.NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        Type_.INTERNAL_NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())),
                pageable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Type save(Type type) {
        return typeRepository.save(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Type update(Type type) {
        return typeRepository.save(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        typeRepository.deleteById(id);
    }

    @Override
    public List<Type> listByCondition(Type type) {
        ExampleMatcher matcher =
                ExampleMatcher.matching()
                        .withIgnoreNullValues()
                        .withMatcher(Type_.NAME, ExampleMatcher.GenericPropertyMatchers.contains())
                        .withMatcher(
                                Type_.INTERNAL_NAME,
                                ExampleMatcher.GenericPropertyMatchers.contains());
        return typeRepository.findAll(Example.of(type, matcher));
    }
}
