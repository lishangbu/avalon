package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.Stat;
import io.github.lishangbu.avalon.dataset.entity.Stat_;
import io.github.lishangbu.avalon.dataset.repository.StatRepository;
import io.github.lishangbu.avalon.dataset.service.StatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// 能力服务实现。
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {
    private final StatRepository statRepository;

    @Override
    public Page<Stat> getPageByCondition(Stat stat, Pageable pageable) {
        return statRepository.findAll(
                Example.of(
                        stat,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        Stat_.NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        Stat_.INTERNAL_NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())),
                pageable);
    }

    @Override
    public List<Stat> listByCondition(Stat stat) {
        ExampleMatcher matcher =
                ExampleMatcher.matching()
                        .withIgnoreNullValues()
                        .withMatcher(
                                Stat_.NAME,
                                ExampleMatcher.GenericPropertyMatchers.contains())
                        .withMatcher(
                                Stat_.INTERNAL_NAME,
                                ExampleMatcher.GenericPropertyMatchers.contains());
        return statRepository.findAll(Example.of(stat, matcher));
    }

    @Override
    public Stat save(Stat stat) {
        return statRepository.save(stat);
    }

    @Override
    public Stat update(Stat stat) {
        return statRepository.save(stat);
    }

    @Override
    public void removeById(Long id) {
        statRepository.deleteById(id);
    }
}
