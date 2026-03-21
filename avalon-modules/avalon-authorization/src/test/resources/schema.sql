CREATE TABLE IF NOT EXISTS menu (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT,
    disabled BOOLEAN,
    extra TEXT,
    icon VARCHAR(200),
    key VARCHAR(200),
    label VARCHAR(200),
    show BOOLEAN,
    path VARCHAR(500),
    name VARCHAR(500),
    redirect VARCHAR(500),
    component VARCHAR(500),
    sorting_order INTEGER,
    pinned BOOLEAN,
    show_tab BOOLEAN,
    enable_multi_tab BOOLEAN
);

CREATE TABLE IF NOT EXISTS oauth_registered_client (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100),
    client_id_issued_at TIMESTAMPTZ,
    client_secret VARCHAR(200),
    client_secret_expires_at TIMESTAMPTZ,
    client_name VARCHAR(200),
    client_authentication_methods VARCHAR(1000),
    authorization_grant_types VARCHAR(1000),
    redirect_uris VARCHAR(1000),
    post_logout_redirect_uris VARCHAR(1000),
    scopes VARCHAR(1000),
    require_proof_key BOOLEAN,
    require_authorization_consent BOOLEAN,
    jwk_set_url VARCHAR(1000),
    token_endpoint_authentication_signing_algorithm VARCHAR(20),
    x509_certificate_subject_dn VARCHAR(20),
    authorization_code_time_to_live VARCHAR(20),
    access_token_time_to_live VARCHAR(20),
    access_token_format VARCHAR(20),
    device_code_time_to_live VARCHAR(20),
    reuse_refresh_tokens BOOLEAN,
    refresh_token_time_to_live VARCHAR(20),
    id_token_signature_algorithm VARCHAR(20),
    x509_certificate_bound_access_tokens BOOLEAN
);

CREATE TABLE IF NOT EXISTS oauth_authorization (
    id VARCHAR(100) PRIMARY KEY,
    registered_client_id VARCHAR(100),
    principal_name VARCHAR(200),
    authorization_grant_type VARCHAR(100),
    authorized_scopes VARCHAR(1000),
    attributes TEXT,
    state VARCHAR(500),
    authorization_code_value VARCHAR(1000),
    authorization_code_issued_at TIMESTAMPTZ,
    authorization_code_expires_at TIMESTAMPTZ,
    authorization_code_metadata TEXT,
    access_token_value VARCHAR(1000),
    access_token_issued_at TIMESTAMPTZ,
    access_token_expires_at TIMESTAMPTZ,
    access_token_metadata TEXT,
    access_token_type VARCHAR(100),
    access_token_scopes VARCHAR(1000),
    oidc_id_token_value VARCHAR(1000),
    oidc_id_token_issued_at TIMESTAMPTZ,
    oidc_id_token_expires_at TIMESTAMPTZ,
    oidc_id_token_metadata TEXT,
    refresh_token_value VARCHAR(1000),
    refresh_token_issued_at TIMESTAMPTZ,
    refresh_token_expires_at TIMESTAMPTZ,
    refresh_token_metadata TEXT,
    user_code_value VARCHAR(1000),
    user_code_issued_at TIMESTAMPTZ,
    user_code_expires_at TIMESTAMPTZ,
    user_code_metadata TEXT,
    device_code_value VARCHAR(1000),
    device_code_issued_at TIMESTAMPTZ,
    device_code_expires_at TIMESTAMPTZ,
    device_code_metadata TEXT
);

CREATE TABLE IF NOT EXISTS oauth_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000),
    PRIMARY KEY (registered_client_id, principal_name)
);

CREATE TABLE IF NOT EXISTS role (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50),
    name VARCHAR(50),
    enabled BOOLEAN
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar VARCHAR(500),
    hashed_password VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS role_menu_relation (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS user_role_relation (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS authentication_log (
    id BIGINT PRIMARY KEY,
    username VARCHAR(20),
    client_id VARCHAR(100),
    grant_type VARCHAR(50),
    ip VARCHAR(512),
    user_agent VARCHAR(512),
    success BOOLEAN,
    error_message VARCHAR(2000),
    occurred_at TIMESTAMPTZ
);
