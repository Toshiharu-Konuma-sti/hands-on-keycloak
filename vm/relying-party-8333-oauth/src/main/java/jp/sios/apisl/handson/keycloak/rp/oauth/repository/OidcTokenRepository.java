package jp.sios.apisl.handson.keycloak.rp.oauth.repository;

import jp.sios.apisl.handson.keycloak.rp.oauth.entity.OidcTokenResponse;

/**
 * OpenID Connect Token Repository Interface
 */
public interface OidcTokenRepository
{
	public OidcTokenResponse generate(String url, String clientId, String clientSecret, String code, String redirect);
	public OidcTokenResponse refresh(String url, String clientId, String clientSecret, String refreshToken);
}
