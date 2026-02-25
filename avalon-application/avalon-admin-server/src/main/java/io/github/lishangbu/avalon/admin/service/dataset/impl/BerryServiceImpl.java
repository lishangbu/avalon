package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.BerryService;
import io.github.lishangbu.avalon.dataset.entity.Berry;
import io.github.lishangbu.avalon.dataset.entity.Berry_;
import io.github.lishangbu.avalon.dataset.repository.BerryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/// Berry 服务实现
///
/// @author lishangbu
/// @since 2025/10/4
@Service
@RequiredArgsConstructor
public class BerryServiceImpl implements BerryService {

    private final BerryRepository berryRepository;

    /// 根据条件分页查询 Berry
    ///
    /// @param berry    查询条件
    /// @param pageable 分页信息
    /// @return 分页结果
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

    /// 新增 Berry
    ///
    /// @param berry 要保存的 Berry 实体
    /// @return 保存后的 Berry 实体
    @Override
    public Berry save(Berry berry) {
        return berryRepository.save(berry);
    }

    /// 根据主键删除 Berry
    ///
    /// @param id 要删除的 Berry 主键
    @Override
    public void removeById(Long id) {
        berryRepository.deleteById(id);
    }

    /// 更新 Berry
    ///
    /// @param berry 要更新的 Berry 实体
    /// @return 更新后的 Berry 实体
    @Override
    public Berry update(Berry berry) {
        return berryRepository.save(berry);
    }
}
