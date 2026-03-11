package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness_;
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository;
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// 树果坚硬度服务实现。
@Service
@RequiredArgsConstructor
public class BerryFirmnessServiceImpl implements BerryFirmnessService {
    private final BerryFirmnessRepository berryFirmnessRepository;

    @Override
    public Page<BerryFirmness> getPageByCondition(BerryFirmness berryFirmness, Pageable pageable) {
        return berryFirmnessRepository.findAll(
                Example.of(
                        berryFirmness,
                        ExampleMatcher.matching()
                                .withMatcher(
                                        BerryFirmness_.NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        BerryFirmness_.INTERNAL_NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withIgnoreNullValues()),
                pageable);
    }

    @Override
    public List<BerryFirmness> listByCondition(BerryFirmness berryFirmness) {
        ExampleMatcher matcher =
                ExampleMatcher.matching()
                        .withIgnoreNullValues()
                        .withMatcher(
                                BerryFirmness_.NAME,
                                ExampleMatcher.GenericPropertyMatchers.contains())
                        .withMatcher(
                                BerryFirmness_.INTERNAL_NAME,
                                ExampleMatcher.GenericPropertyMatchers.contains());
        return berryFirmnessRepository.findAll(Example.of(berryFirmness, matcher));
    }

    @Override
    public BerryFirmness save(BerryFirmness berryFirmness) {
        return berryFirmnessRepository.save(berryFirmness);
    }

    @Override
    public BerryFirmness update(BerryFirmness berryFirmness) {
        return berryFirmnessRepository.save(berryFirmness);
    }

    @Override
    public void removeById(Long id) {
        berryFirmnessRepository.deleteById(id);
    }
}
