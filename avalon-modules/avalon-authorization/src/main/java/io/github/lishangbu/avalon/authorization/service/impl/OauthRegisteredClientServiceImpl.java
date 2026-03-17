package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient_;
import io.github.lishangbu.avalon.authorization.repository.Oauth2RegisteredClientRepository;
import io.github.lishangbu.avalon.authorization.service.OauthRegisteredClientService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// OAuth2 注册客户端服务实现
///
/// @author lishangbu
/// @since 2026/3/19
@Service
@RequiredArgsConstructor
public class OauthRegisteredClientServiceImpl implements OauthRegisteredClientService {

    private final Oauth2RegisteredClientRepository oauth2RegisteredClientRepository;

    @Override
    public Page<OauthRegisteredClient> getPageByCondition(
            OauthRegisteredClient registeredClient, Pageable pageable) {
        return oauth2RegisteredClientRepository.findAll(
                Example.of(
                        registeredClient,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        OauthRegisteredClient_.CLIENT_ID,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        OauthRegisteredClient_.CLIENT_NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())),
                pageable);
    }

    @Override
    public List<OauthRegisteredClient> listByCondition(OauthRegisteredClient registeredClient) {
        return oauth2RegisteredClientRepository.findAll(
                Example.of(
                        registeredClient,
                        ExampleMatcher.matching()
                                .withIgnoreNullValues()
                                .withMatcher(
                                        OauthRegisteredClient_.CLIENT_ID,
                                        ExampleMatcher.GenericPropertyMatchers.contains())
                                .withMatcher(
                                        OauthRegisteredClient_.CLIENT_NAME,
                                        ExampleMatcher.GenericPropertyMatchers.contains())));
    }

    @Override
    public Optional<OauthRegisteredClient> getById(String id) {
        return oauth2RegisteredClientRepository.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OauthRegisteredClient save(OauthRegisteredClient registeredClient) {
        if (registeredClient.getId() == null || registeredClient.getId().isBlank()) {
            registeredClient.setId(UUID.randomUUID().toString());
        }
        if (registeredClient.getClientIdIssuedAt() == null) {
            registeredClient.setClientIdIssuedAt(Instant.now());
        }
        return oauth2RegisteredClientRepository.save(registeredClient);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OauthRegisteredClient update(OauthRegisteredClient registeredClient) {
        return oauth2RegisteredClientRepository.save(registeredClient);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(String id) {
        oauth2RegisteredClientRepository.deleteById(id);
    }
}
