-- 用户信息表
CREATE TABLE "user" (
    id BIGINT PRIMARY KEY NOT NULL,
    username VARCHAR(20) NOT NULL DEFAULT '',
    password VARCHAR(100) NOT NULL DEFAULT '',
    UNIQUE(username)
);

-- 角色信息表
CREATE TABLE role (
    id BIGINT PRIMARY KEY NOT NULL,
    code VARCHAR(50) NOT NULL DEFAULT '',
    name VARCHAR(50) NOT NULL DEFAULT '',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(code)
);

-- 用户角色关系表
CREATE TABLE user_role_relation (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

-- 角色权限关系表
CREATE TABLE role_permission_relation (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);

-- 权限表
CREATE TABLE permission (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- 用户授权确认表
CREATE TABLE oauth_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- 用户认证信息表
CREATE TABLE oauth_authorization (
    id VARCHAR(100) PRIMARY KEY NOT NULL,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000),
    attributes VARCHAR(10000),
    state VARCHAR(500),
    authorization_code_value VARCHAR(1000),
    authorization_code_issued_at TIMESTAMP,
    authorization_code_expires_at TIMESTAMP,
    authorization_code_metadata VARCHAR(10000)
);

-- Oauth2注册客户端表
CREATE TABLE oauth_registered_client (
    id VARCHAR(100) PRIMARY KEY NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP NOT NULL,
    client_secret VARCHAR(200),
    client_secret_expires_at TIMESTAMP,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000),
    post_logout_redirect_uris VARCHAR(1000),
    scopes VARCHAR(1000) NOT NULL,
    require_proof_key BOOLEAN DEFAULT FALSE NOT NULL,
    require_authorization_consent BOOLEAN DEFAULT FALSE NOT NULL,
    jwk_set_url VARCHAR(1000),
    token_endpoint_authentication_signing_algorithm VARCHAR(20),
    x509_certificate_subject_dn VARCHAR(20),
    authorization_code_time_to_live VARCHAR(20) DEFAULT '5m' NOT NULL,
    access_token_time_to_live VARCHAR(20) DEFAULT '5m' NOT NULL,
    access_token_format VARCHAR(20) DEFAULT 'self-contained' NOT NULL,
    device_code_time_to_live VARCHAR(20) DEFAULT '5m' NOT NULL,
    reuse_refresh_tokens BOOLEAN DEFAULT TRUE NOT NULL,
    refresh_token_time_to_live VARCHAR(20) DEFAULT '1h' NOT NULL,
    id_token_signature_algorithm VARCHAR(20) DEFAULT 'RS256' NOT NULL,
    x509_certificate_bound_access_tokens BOOLEAN DEFAULT FALSE NOT NULL
);
