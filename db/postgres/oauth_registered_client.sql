BEGIN;
insert into public.oauth_registered_client (id, access_token_format, access_token_time_to_live,
                                            authorization_code_time_to_live, authorization_grant_types,
                                            client_authentication_methods, client_id, client_id_issued_at, client_name,
                                            client_secret, client_secret_expires_at, device_code_time_to_live,
                                            id_token_signature_algorithm, jwk_set_url, post_logout_redirect_uris,
                                            redirect_uris, refresh_token_time_to_live, require_authorization_consent,
                                            require_proof_key, reuse_refresh_tokens, scopes,
                                            token_endpoint_authentication_signing_algorithm,
                                            x509_certificate_bound_access_tokens, x509_certificate_subject_dn)
values ('1', 'self-contained', '2h', '2h', 'refresh_token,client_credentials,password',
        'client_secret_basic,client_secret_post,client_secret_jwt', 'client', '2025-08-12 08:11:22.000000 +00:00',
        '测试客户端的客户端', '{noop}client', '9999-08-12 08:11:22.000000 +00:00', '1h', 'RS256', '',
        'http://localhost:8080', '', '30d', false, false, true, 'openid,profile', 'RS256', false, ''),
       ('2', 'reference', '2h', '2h', 'refresh_token,client_credentials,password',
        'client_secret_basic,client_secret_post,client_secret_jwt', 'test', '2025-08-12 08:11:22.000000 +00:00',
        '测试REFERENCE模式的客户端', '{noop}test', '9999-08-12 08:11:22.000000 +00:00', '1h', 'RS256', '',
        'http://localhost:8080', '', '30d', false, false, true, 'openid,profile', 'RS256', false, '');
COMMIT;