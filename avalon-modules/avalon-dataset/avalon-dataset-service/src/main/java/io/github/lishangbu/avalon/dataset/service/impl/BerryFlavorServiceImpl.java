package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor_;
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository;
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// 树果风味服务实现。
@Service
@RequiredArgsConstructor
public class BerryFlavorServiceImpl implements BerryFlavorService {

    private final BerryFlavorRepository berryFlavorRepository;

    @Override
    public Page<BerryFlavor> getPageByCondition(BerryFlavor berryFlavor, Pageable pageable) {
        return berryFlavorRepository.findAll(
                Example.of(
                        berryFlavor,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        BerryFlavor_.NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        BerryFlavor_.INTERNAL_NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())),
                pageable);
    }

    @Override
    public BerryFlavor save(BerryFlavor berryFlavor) {
        return berryFlavorRepository.save(berryFlavor);
    }

    @Override
    public BerryFlavor update(BerryFlavor berryFlavor) {
        return berryFlavorRepository.save(berryFlavor);
    }

    @Override
    public void removeById(Long id) {
        berryFlavorRepository.deleteById(id);
    }
}
