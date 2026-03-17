package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass_;
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository;
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// 招式伤害类别服务实现。
@Service
@RequiredArgsConstructor
public class MoveDamageClassServiceImpl implements MoveDamageClassService {
    private final MoveDamageClassRepository moveDamageClassRepository;

    @Override
    public Page<MoveDamageClass> getPageByCondition(
            MoveDamageClass moveDamageClass, Pageable pageable) {
        return moveDamageClassRepository.findAll(
                Example.of(
                        moveDamageClass,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        MoveDamageClass_.NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        MoveDamageClass_.INTERNAL_NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())),
                pageable);
    }

    @Override
    public List<MoveDamageClass> listByCondition(MoveDamageClass moveDamageClass) {
        ExampleMatcher matcher =
                ExampleMatcher.matching()
                        .withIgnoreNullValues()
                        .withMatcher(
                                MoveDamageClass_.NAME,
                                ExampleMatcher.GenericPropertyMatchers.contains())
                        .withMatcher(
                                MoveDamageClass_.INTERNAL_NAME,
                                ExampleMatcher.GenericPropertyMatchers.contains());
        return moveDamageClassRepository.findAll(Example.of(moveDamageClass, matcher));
    }

    @Override
    public MoveDamageClass save(MoveDamageClass moveDamageClass) {
        return moveDamageClassRepository.save(moveDamageClass);
    }

    @Override
    public MoveDamageClass update(MoveDamageClass moveDamageClass) {
        return moveDamageClassRepository.save(moveDamageClass);
    }

    @Override
    public void removeById(Long id) {
        moveDamageClassRepository.deleteById(id);
    }
}
