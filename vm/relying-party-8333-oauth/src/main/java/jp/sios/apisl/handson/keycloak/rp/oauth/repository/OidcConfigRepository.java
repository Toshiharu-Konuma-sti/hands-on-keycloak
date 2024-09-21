package jp.sios.apisl.handson.keycloak.rp.oauth.repository;

import jp.sios.apisl.handson.keycloak.rp.oauth.entity.OidcConfigResponse;

/**
 * OpenID Connect Configuration Repository Interface
 */
public interface OidcConfigRepository
{
	public OidcConfigResponse discovery();
}
