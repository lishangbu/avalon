package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.entity.Berry_;
import io.github.lishangbu.avalon.dataset.repository.BerryRepository;
import io.github.lishangbu.avalon.dataset.service.BerryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// 树果服务实现。
@Service
@RequiredArgsConstructor
public class BerryServiceImpl implements BerryService {
    private final BerryRepository berryRepository;

    @Override
    public Page<Berry> getPageByCondition(Berry berry, Pageable pageable) {
        return berryRepository.findAll(
                Example.of(
                        berry,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        Berry_.NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        Berry_.INTERNAL_NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())),
                pageable);
    }

    @Override
    public Berry save(Berry berry) {
        return berryRepository.save(berry);
    }

    @Override
    public Berry update(Berry berry) {
        return berryRepository.save(berry);
    }

    @Override
    public void removeById(Long id) {
        berryRepository.deleteById(id);
    }
}
