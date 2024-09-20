package jp.sios.apisl.handson.keycloak.rlypty.repository;

import jp.sios.apisl.handson.keycloak.rlypty.entity.OidcConfigResponse;

/**
 * OpenID Connect Configuration Repository Interface
 */
public interface OidcConfigRepository
{
	public OidcConfigResponse discovery();
}
