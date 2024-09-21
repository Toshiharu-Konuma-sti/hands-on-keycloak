package jp.sios.apisl.handson.keycloak.rp.auth.repository;

import jp.sios.apisl.handson.keycloak.rp.auth.entity.OidcConfigResponse;

/**
 * OpenID Connect Configuration Repository Interface
 */
public interface OidcConfigRepository
{
	public OidcConfigResponse discovery();
}
